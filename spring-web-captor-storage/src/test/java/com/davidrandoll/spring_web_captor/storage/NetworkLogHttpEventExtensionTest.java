package com.davidrandoll.spring_web_captor.storage;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static com.davidrandoll.spring_web_captor.storage.NetworkLogHttpEventExtension.REQUEST_ID_KEY;
import static org.assertj.core.api.Assertions.assertThat;

class NetworkLogHttpEventExtensionTest {

    private final NetworkLogHttpEventExtension extension =
            new NetworkLogHttpEventExtension(RequestIdProvider.uuid());

    @Test
    void requestEnrichmentGeneratesAndStashesRequestId() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        Map<String, Object> data = extension.enrichRequestEvent(req, res, null);

        Object id = data.get(REQUEST_ID_KEY);
        assertThat(id).isInstanceOf(String.class);
        assertThat((String) id).isNotBlank();
        // id is stashed on the request so a later dispatch reuses it
        assertThat(req.getAttribute(REQUEST_ID_KEY)).isEqualTo(id);
    }

    @Test
    void requestEnrichmentReusesExistingRequestId() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute(REQUEST_ID_KEY, "existing-id");
        MockHttpServletResponse res = new MockHttpServletResponse();

        Map<String, Object> data = extension.enrichRequestEvent(req, res, null);

        assertThat(data.get(REQUEST_ID_KEY)).isEqualTo("existing-id");
    }

    @Test
    void requestEnrichmentIgnoresBlankExistingRequestId() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute(REQUEST_ID_KEY, "");
        MockHttpServletResponse res = new MockHttpServletResponse();

        Map<String, Object> data = extension.enrichRequestEvent(req, res, null);

        assertThat((String) data.get(REQUEST_ID_KEY)).isNotBlank();
    }

    @Test
    void responseEnrichmentCarriesRequestIdForward() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute(REQUEST_ID_KEY, "carry-id");
        MockHttpServletResponse res = new MockHttpServletResponse();

        Map<String, Object> data = extension.enrichResponseEvent(req, res, null, null);

        assertThat(data.get(REQUEST_ID_KEY)).isEqualTo("carry-id");
    }

    @Test
    void responseEnrichmentWithoutRequestIdReturnsEmptyData() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        Map<String, Object> data = extension.enrichResponseEvent(req, res, null, null);

        assertThat(data).doesNotContainKey(REQUEST_ID_KEY);
    }
}
