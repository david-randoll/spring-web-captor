package com.davidrandoll.spring_web_captor.nonexistent_endpoint;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.setup.EventCaptureListener;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests that request body is correctly captured even when the endpoint doesn't exist (404).
 * These tests target Bug 1: HttpResponseEventPublisher.doFilterInternal passes unwrapped
 * request/response to publishRequestEvent, causing the response event's embedded request
 * body to be empty.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NonExistentEndpointBodyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventCaptureListener eventCaptureListener;

    @BeforeEach
    void setUp() {
        eventCaptureListener.clearEvents();
    }

    @Test
    void testPostTo404PreservesRequestBodyInResponseEvent() throws Exception {
        String body = "{\"name\":\"test\"}";

        mockMvc.perform(post("/nonexistent/path")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());

        assertThat(eventCaptureListener.getResponseEvents()).hasSize(1);
        HttpResponseEvent responseEvent = eventCaptureListener.getResponseEvents().getFirst();

        // The response event should contain the request body from the original request
        JsonNode requestBody = responseEvent.getRequestBody();
        assertThat(requestBody.isMissingNode()).isFalse();
        assertThat(requestBody.isNull()).isFalse();
        assertThat(requestBody.has("name")).isTrue();
        assertThat(requestBody.get("name").asText()).isEqualTo("test");
    }

    @Test
    void testRequestAndResponseEventBodiesMatchFor404Post() throws Exception {
        String body = "{\"key\":\"value\"}";

        mockMvc.perform(post("/nonexistent/another-path")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());

        assertThat(eventCaptureListener.getRequestEvents()).hasSize(1);
        assertThat(eventCaptureListener.getResponseEvents()).hasSize(1);

        HttpRequestEvent requestEvent = eventCaptureListener.getRequestEvents().getFirst();
        HttpResponseEvent responseEvent = eventCaptureListener.getResponseEvents().getFirst();

        // Both events should have the same request body
        JsonNode requestEventBody = requestEvent.getRequestBody();
        JsonNode responseEventBody = responseEvent.getRequestBody();

        assertThat(requestEventBody.has("key")).isTrue();
        assertThat(responseEventBody.has("key")).isTrue();
        assertThat(responseEventBody.get("key").asText())
                .isEqualTo(requestEventBody.get("key").asText());
    }

    @Test
    void testPutTo404PreservesRequestBody() throws Exception {
        String body = "update data";

        mockMvc.perform(put("/nonexistent/resource")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(body))
                .andExpect(status().isNotFound());

        assertThat(eventCaptureListener.getResponseEvents()).hasSize(1);
        HttpResponseEvent responseEvent = eventCaptureListener.getResponseEvents().getFirst();

        JsonNode requestBody = responseEvent.getRequestBody();
        assertThat(requestBody.asText()).isEqualTo("update data");
    }
}
