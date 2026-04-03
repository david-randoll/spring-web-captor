package com.davidrandoll.spring_web_captor.endpoint_called;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.setup.EventCaptureListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that endpointCalled is true when a handler was invoked and false for 404s.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EndpointCalledFlagTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventCaptureListener eventCaptureListener;

    @Autowired
    private org.springframework.boot.test.web.client.TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        eventCaptureListener.clearEvents();
    }

    @Test
    void testEndpointCalledTrue_forRealEndpoint() throws Exception {
        mockMvc.perform(get("/test/get"))
                .andExpect(status().isOk());

        assertThat(eventCaptureListener.getRequestEvents()).hasSize(1);
        assertThat(eventCaptureListener.getResponseEvents()).hasSize(1);

        HttpRequestEvent requestEvent = eventCaptureListener.getRequestEvents().getFirst();
        HttpResponseEvent responseEvent = eventCaptureListener.getResponseEvents().getFirst();

        assertThat(requestEvent.isEndpointCalled()).isTrue();
        assertThat(responseEvent.isEndpointCalled()).isTrue();
    }

    @Test
    void testEndpointCalledFalse_for405MethodNotAllowed() throws Exception {
        // POST to /test/error/runtime which only supports GET -> 405
        // The handler was never invoked, so endpointCalled should be false
        mockMvc.perform(post("/test/error/runtime"))
                .andExpect(status().isMethodNotAllowed());

        assertThat(eventCaptureListener.getRequestEvents()).hasSize(1);
        HttpRequestEvent requestEvent = eventCaptureListener.getRequestEvents().getFirst();

        assertThat(requestEvent.isEndpointCalled()).isFalse();
    }

    @Test
    void testEndpointCalledFalse_for415UnsupportedMediaType() throws Exception {
        // POST with wrong content type -> 415, handler was never invoked
        mockMvc.perform(post("/test/http-methods/with-content-type")
                        .contentType("application/unsupported-type")
                        .content("data"))
                .andExpect(status().isUnsupportedMediaType());

        assertThat(eventCaptureListener.getRequestEvents()).hasSize(1);
        HttpRequestEvent requestEvent = eventCaptureListener.getRequestEvents().getFirst();

        assertThat(requestEvent.isEndpointCalled()).isFalse();
    }

    // ---- Multipart file upload bug (real server only) ----
    // In a real server, DispatcherServlet wraps CachedBodyHttpServletRequest in
    // StandardMultipartHttpServletRequest before calling the interceptor.
    // The interceptor's toCachedBodyHttpServletRequest() creates a NEW wrapper
    // (since the multipart wrapper is not a CachedBodyHttpServletRequest), so
    // setEndpointCalled(true) and markAsPublished() are set on the wrong wrapper.
    // This causes: (1) endpointCalled=false on the real event, (2) duplicate events.
    // MockMvc hides this because MockMultipartHttpServletRequest is already a
    // MultipartHttpServletRequest, so DispatcherServlet skips re-wrapping.

    private void sendMultipartToRealServer() {
        org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
        body.add("file", new org.springframework.core.io.ByteArrayResource("file content".getBytes()) {
            @Override
            public String getFilename() {
                return "test.txt";
            }
        });
        body.add("description", "test");

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var requestEntity = new org.springframework.http.HttpEntity<>(body, headers);
        var response = restTemplate.postForEntity("/test/body/multipart", requestEntity, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        org.awaitility.Awaitility.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(eventCaptureListener.getResponseEvents()).isNotEmpty()
        );
    }

    @Test
    void testMultipartPost_shouldPublishExactlyOneRequestEvent() {
        sendMultipartToRealServer();

        assertThat(eventCaptureListener.getRequestEvents())
                .as("Should publish exactly 1 request event, not duplicates")
                .hasSize(1);
    }

    @Test
    void testMultipartPost_shouldPublishExactlyOneResponseEvent() {
        sendMultipartToRealServer();

        assertThat(eventCaptureListener.getResponseEvents())
                .as("Should publish exactly 1 response event, not duplicates")
                .hasSize(1);
    }

    @Test
    void testMultipartPost_requestEventEndpointCalledShouldBeTrue() {
        sendMultipartToRealServer();

        HttpRequestEvent requestEvent = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(requestEvent.isEndpointCalled())
                .as("Multipart endpoint was called and returned 200, request event endpointCalled should be true")
                .isTrue();
    }

    @Test
    void testMultipartPost_responseEventEndpointCalledShouldBeTrue() {
        sendMultipartToRealServer();

        HttpResponseEvent responseEvent = eventCaptureListener.getResponseEvents().getFirst();
        assertThat(responseEvent.isEndpointCalled())
                .as("Multipart endpoint was called and returned 200, response event endpointCalled should be true")
                .isTrue();
    }

    @Test
    void testEndpointCalledFalse_for404() throws Exception {
        mockMvc.perform(get("/completely-nonexistent-path"))
                .andExpect(status().isNotFound());

        assertThat(eventCaptureListener.getRequestEvents()).hasSize(1);
        assertThat(eventCaptureListener.getResponseEvents()).hasSize(1);

        HttpRequestEvent requestEvent = eventCaptureListener.getRequestEvents().getFirst();
        HttpResponseEvent responseEvent = eventCaptureListener.getResponseEvents().getFirst();

        assertThat(requestEvent.isEndpointCalled()).isFalse();
        assertThat(responseEvent.isEndpointCalled()).isFalse();
    }
}
