package com.davidrandoll.spring_web_captor.publisher.request;

import com.davidrandoll.spring_web_captor.body_parser.registry.DefaultBodyParserRegistry;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.field_captor.captors.RequestBodyCaptor;
import com.davidrandoll.spring_web_captor.properties.WebCaptorProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * A request whose body nobody read during the chain, and whose container stream is already closed by the time the
 * response publisher asks for it (a body-less GET, or an empty body a JSON reader consumed and auto-closed). That is
 * "no body", not an error: the captor used to throw {@code IOException: Stream closed} here, which the field-captor
 * registry logged at ERROR on every such request.
 */
class CachedBodyHttpServletRequestTest {

    /** Tomcat's input after the request is done with it: every read refuses. */
    private static MockHttpServletRequest closedStream(String method) {
        return new MockHttpServletRequest(method, "/anything") {
            @Override
            public ServletInputStream getInputStream() {
                return new ServletInputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("Stream closed");
                    }

                    @Override
                    public boolean isFinished() {
                        return false;
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setReadListener(ReadListener listener) {
                    }
                };
            }
        };
    }

    @Test
    void aClosedStreamWithNothingCachedIsAnEmptyBody() {
        CachedBodyHttpServletRequest request = new CachedBodyHttpServletRequest(closedStream("GET"));

        assertThatCode(request::getCachedBody).doesNotThrowAnyException();
        assertThat(request.getCachedBody()).isEmpty();
    }

    @Test
    void theBodyCaptorRecordsNoBodyRatherThanFailing() {
        RequestBodyCaptor captor = new RequestBodyCaptor(
                new DefaultBodyParserRegistry(new ObjectMapper(), new WebCaptorProperties.EventDetails()));
        CachedBodyHttpServletRequest request = new CachedBodyHttpServletRequest(closedStream("POST"));

        assertThatCode(() -> captor.capture(request, HttpRequestEvent.builder())).doesNotThrowAnyException();
    }

    @Test
    void aReadableBodyIsStillCaptured() {
        MockHttpServletRequest raw = new MockHttpServletRequest("POST", "/anything");
        raw.setContent("{\"a\":1}".getBytes(StandardCharsets.UTF_8));
        raw.setContentType("application/json");

        assertThat(new String(new CachedBodyHttpServletRequest(raw).getCachedBody(), StandardCharsets.UTF_8))
                .isEqualTo("{\"a\":1}");
    }
}
