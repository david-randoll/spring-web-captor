package com.davidrandoll.spring_web_captor.query_params;

import com.davidrandoll.spring_web_captor.field_captor.captors.RequestQueryParamsCaptor;
import com.davidrandoll.spring_web_captor.publisher.request.CachedBodyHttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RequestQueryParamsCaptor}.
 *
 * <p>The native-only bug: under GraalVM native image, {@code HttpServletRequest.getParameterMap()} came back
 * EMPTY for a request that plainly had a query string (the workflow-engine {@code getParticipantProfile}
 * path captured {@code ?entityId=...} as {@code {}}), so the participant id resolved to "" and the request
 * 500'd. This is reproduced on the JVM by a {@link MockHttpServletRequest} that has a query string but no
 * registered parameters — exactly the shape the native container produced.</p>
 */
class RequestQueryParamsCaptorTest {

    private static final String UUID = "0002189f-60d2-4431-9b02-d02b93fc569e";

    private final RequestQueryParamsCaptor captor = new RequestQueryParamsCaptor();

    @Test
    @DisplayName("captures params from getParameterMap (normal JVM path)")
    void capturesFromParameterMap() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/license/workflow-engine/getParticipantProfile");
        request.setParameter("entityId", UUID);
        request.setParameter("entityType", "individual");

        MultiValueMap<String, String> params = captor.getRequestParams(request);

        assertThat(params.getFirst("entityId")).isEqualTo(UUID);
        assertThat(params.getFirst("entityType")).isEqualTo("individual");
    }

    @Test
    @DisplayName("REGRESSION: falls back to the raw query string when getParameterMap is empty (native condition)")
    void capturesFromQueryStringWhenParameterMapEmpty() {
        // Simulate the native container: query string present, but getParameterMap() returns {}.
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/license/workflow-engine/getParticipantProfile");
        request.setQueryString("entityId=" + UUID + "&entityType=individual&include=profile");
        // deliberately NO setParameter(...) -> getParameterMap() is empty

        assertThat(request.getParameterMap()).isEmpty(); // guards the precondition this test relies on

        MultiValueMap<String, String> params = captor.getRequestParams(request);

        // Without the query-string fallback this map is empty and these assertions fail.
        assertThat(params.getFirst("entityId")).isEqualTo(UUID);
        assertThat(params.getFirst("entityType")).isEqualTo("individual");
        assertThat(params.getFirst("include")).isEqualTo("profile");
    }

    @Test
    @DisplayName("query-string fallback URL-decodes keys and values")
    void fallbackUrlDecodes() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.setQueryString("q=hello%20world&tag=a%26b");

        MultiValueMap<String, String> params = captor.getRequestParams(request);

        assertThat(params.getFirst("q")).isEqualTo("hello world");
        assertThat(params.getFirst("tag")).isEqualTo("a&b");
    }

    @Test
    @DisplayName("query-string fallback preserves repeated (multi-value) keys")
    void fallbackKeepsMultiValueKeys() {
        // FAIL-WITHOUT-FIX: the pre-fix fallback used Map semantics and would drop all but one value of `id`.
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.setQueryString("id=1&id=2&id=3&type=dog");

        MultiValueMap<String, String> params = captor.getRequestParams(request);

        assertThat(params.get("id")).containsExactly("1", "2", "3");
        assertThat(params.getFirst("type")).isEqualTo("dog");
    }

    @Test
    @DisplayName("REGRESSION: falls back to the form-encoded POST body when getParameterMap is empty (native condition)")
    void fallbackParsesFormUrlEncodedBody() {
        // Simulate the native container for a form POST: form body present, but getParameterMap() returns {}.
        // FAIL-WITHOUT-FIX: the pre-fix fallback only read getQueryString() and returned {} here, losing the body params.
        MockHttpServletRequest delegate = new MockHttpServletRequest("POST", "/api/form");
        delegate.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        delegate.setContent(("entityId=" + UUID + "&role=admin&role=editor").getBytes(StandardCharsets.UTF_8));
        // deliberately NO setParameter(...) -> getParameterMap() is empty

        CachedBodyHttpServletRequest request = new CachedBodyHttpServletRequest(delegate);
        assertThat(request.getParameterMap()).isEmpty(); // guards the precondition this test relies on

        MultiValueMap<String, String> params = captor.getRequestParams(request);

        assertThat(params.getFirst("entityId")).isEqualTo(UUID);
        assertThat(params.get("role")).containsExactly("admin", "editor");
    }

    @Test
    @DisplayName("form-body fallback merges query-string and body params (mirrors getParameterMap on the JVM)")
    void fallbackMergesQueryAndBody() {
        MockHttpServletRequest delegate = new MockHttpServletRequest("POST", "/api/form");
        delegate.setQueryString("source=web");
        delegate.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        delegate.setContent("name=fido".getBytes(StandardCharsets.UTF_8));

        CachedBodyHttpServletRequest request = new CachedBodyHttpServletRequest(delegate);

        MultiValueMap<String, String> params = captor.getRequestParams(request);

        assertThat(params.getFirst("source")).isEqualTo("web");
        assertThat(params.getFirst("name")).isEqualTo("fido");
    }
}
