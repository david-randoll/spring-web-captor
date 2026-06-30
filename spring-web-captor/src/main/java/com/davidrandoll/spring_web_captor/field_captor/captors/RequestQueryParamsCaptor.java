package com.davidrandoll.spring_web_captor.field_captor.captors;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.field_captor.IRequestFieldCaptor;
import com.davidrandoll.spring_web_captor.publisher.request.CachedBodyHttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class RequestQueryParamsCaptor implements IRequestFieldCaptor {

    private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";

    @Override
    public void capture(HttpServletRequest request, HttpRequestEvent.HttpRequestEventBuilder<?, ?> builder) {
        var params = this.getRequestParams(request);
        builder.queryParams(params);
    }

    public MultiValueMap<String, String> getRequestParams(HttpServletRequest request) {
        var fromParameterMap = this.fromParameterMap(request);
        // GraalVM native image: the servlet container's getParameterMap() can come back EMPTY for a request
        // that plainly has a query string (observed on the workflow-engine getParticipantProfile path —
        // ?entityId=... captured as {}). The single library-relevant native build hint that would force
        // Tomcat's lazy query parser to populate (-H:+AddAllCharsets) is already present in the consuming
        // service, yet the map is still empty under native — so Tomcat's lazy parameter parser cannot be
        // relied on under native and the raw fallback below is the PRIMARY mechanism there, not a guard.
        //
        // The fallback reconstructs what getParameterMap() returns on the JVM: it merges the URI query
        // string AND (for x-www-form-urlencoded POSTs) the form body, preserves multi-value keys, and
        // URL-decodes using the request's character encoding. It depends only on String ops + a byte->String
        // decode, so it is native-safe. On the JVM the parameter map is populated, so the fallback is a no-op.
        if (!fromParameterMap.isEmpty()) {
            return fromParameterMap;
        }
        return this.fromRawRequest(request);
    }

    private MultiValueMap<String, String> fromParameterMap(HttpServletRequest request) {
        return request.getParameterMap()
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() != null ? Arrays.asList(entry.getValue()) : Collections.<String>emptyList(),
                        (a, b) -> b,
                        LinkedMultiValueMap::new
                ));
    }

    /**
     * Native-safe reconstruction of the request parameters when {@link HttpServletRequest#getParameterMap()}
     * yields nothing. Mirrors the servlet contract for {@code getParameterMap()}: query-string parameters are
     * merged with {@code application/x-www-form-urlencoded} body parameters (POST only), keeping every value of
     * a repeated key.
     */
    private MultiValueMap<String, String> fromRawRequest(HttpServletRequest request) {
        Charset charset = resolveCharset(request);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        // Query-string params (from the URI — independent of the request body).
        parseUrlEncoded(request.getQueryString(), charset, params);

        // Form-encoded POST body params — getParameterMap() merges these on the JVM, so mirror it.
        if (isFormPost(request)) {
            parseUrlEncoded(readFormBody(request, charset), charset, params);
        }

        return params;
    }

    private static boolean isFormPost(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null
                && contentType.toLowerCase().contains(FORM_CONTENT_TYPE)
                && "POST".equalsIgnoreCase(request.getMethod());
    }

    /**
     * Reads the cached request body (never the live stream) so the body can still be read again by the
     * downstream body captor. Only the {@link CachedBodyHttpServletRequest} wrapper guarantees a re-readable
     * body; for any other request type the form body is skipped rather than risking stream consumption.
     */
    private String readFormBody(HttpServletRequest request, Charset charset) {
        try {
            if (request instanceof CachedBodyHttpServletRequest cached) {
                byte[] body = cached.getCachedBody();
                if (body != null && body.length > 0) {
                    return new String(body, charset);
                }
            }
        } catch (Exception e) {
            log.debug("Could not read cached form body for query-param fallback: {}", e.getMessage());
        }
        return null;
    }

    private static Charset resolveCharset(HttpServletRequest request) {
        try {
            String encoding = request.getCharacterEncoding();
            if (encoding != null && !encoding.isBlank()) {
                return Charset.forName(encoding);
            }
        } catch (Exception e) {
            // Unsupported/illegal charset name — fall through to UTF-8.
        }
        return StandardCharsets.UTF_8;
    }

    /**
     * Parse {@code key=value&key2=value2} text into the supplied multi-value map, URL-decoding keys and values
     * with the given charset and preserving repeated keys.
     */
    private void parseUrlEncoded(String raw, Charset charset, MultiValueMap<String, String> params) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String rawKey = eq >= 0 ? pair.substring(0, eq) : pair;
            String rawValue = eq >= 0 ? pair.substring(eq + 1) : "";
            String key = URLDecoder.decode(rawKey, charset);
            String value = URLDecoder.decode(rawValue, charset);
            if (!key.isEmpty()) {
                params.add(key, value);
            }
        }
    }
}
