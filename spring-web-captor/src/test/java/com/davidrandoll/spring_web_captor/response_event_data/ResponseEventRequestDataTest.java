package com.davidrandoll.spring_web_captor.response_event_data;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
import com.davidrandoll.spring_web_captor.event.HttpMethodEnum;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that response events correctly carry all request data
 * (path, method, headers, queryParams, pathParams, body).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResponseEventRequestDataTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventCaptureListener eventCaptureListener;

    @BeforeEach
    void setUp() {
        eventCaptureListener.clearEvents();
    }

    private HttpResponseEvent performPostAndGetResponseEvent() throws Exception {
        String body = "{\"field\":\"value\"}";

        mockMvc.perform(post("/test/response-data/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .param("search", "term")
                        .header("X-Custom-Header", "CustomValue"))
                .andExpect(status().isOk());

        assertThat(eventCaptureListener.getResponseEvents()).hasSize(1);
        return eventCaptureListener.getResponseEvents().getFirst();
    }

    @Test
    void testResponseEventContainsRequestPath() throws Exception {
        HttpResponseEvent event = performPostAndGetResponseEvent();
        assertThat(event.getPath()).isEqualTo("/test/response-data/42");
    }

    @Test
    void testResponseEventContainsRequestMethod() throws Exception {
        HttpResponseEvent event = performPostAndGetResponseEvent();
        assertThat(event.getMethod()).isEqualTo(HttpMethodEnum.POST);
    }

    @Test
    void testResponseEventContainsRequestHeaders() throws Exception {
        HttpResponseEvent event = performPostAndGetResponseEvent();
        assertThat(event.getHeaders()).containsEntry("X-Custom-Header", List.of("CustomValue"));
    }

    @Test
    void testResponseEventContainsQueryParams() throws Exception {
        HttpResponseEvent event = performPostAndGetResponseEvent();
        assertThat(event.getQueryParams()).containsEntry("search", List.of("term"));
    }

    @Test
    void testResponseEventContainsPathParams() throws Exception {
        HttpResponseEvent event = performPostAndGetResponseEvent();
        assertThat(event.getPathParams()).containsEntry("id", "42");
    }

    @Test
    void testResponseEventContainsRequestBody() throws Exception {
        HttpResponseEvent event = performPostAndGetResponseEvent();
        JsonNode requestBody = event.getRequestBody();
        assertThat(requestBody.has("field")).isTrue();
        assertThat(requestBody.get("field").asText()).isEqualTo("value");
    }
}
