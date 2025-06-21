package com.davidrandoll.spring_web_captor.http_method;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
import com.davidrandoll.spring_web_captor.event.HttpMethodEnum;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.setup.EventCaptureListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpMethodsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventCaptureListener eventCaptureListener; // Capture published events

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        eventCaptureListener.clearEvents(); // Reset events before each test
    }

    @Test
    void testGetMethodCaptured() throws Exception {
        mockMvc.perform(get("/test/http-methods"))
                .andExpect(status().isOk())
                .andExpect(content().string("GET response"));

        HttpRequestEvent requestEvent = eventCaptureListener.getRequestEvents().getFirst();
        HttpResponseEvent responseEvent = eventCaptureListener.getResponseEvents().getFirst();

        assertThat(requestEvent.getMethod()).isEqualTo(HttpMethodEnum.GET);
        assertThat(requestEvent.getPath()).isEqualTo("/test/http-methods");
        assertThat(responseEvent.getResponseBody().asText()).isEqualTo("GET response");
    }

    @Test
    void testPostMethodCaptured() throws Exception {
        String body = "hello";
        mockMvc.perform(post("/test/http-methods")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("POST response: hello"));

        HttpRequestEvent requestEvent = eventCaptureListener.getRequestEvents().getFirst();
        HttpResponseEvent responseEvent = eventCaptureListener.getResponseEvents().getFirst();

        assertThat(requestEvent.getMethod()).isEqualTo(HttpMethodEnum.POST);
        assertThat(requestEvent.getRequestBody().asText()).isEqualTo("hello");
        assertThat(responseEvent.getResponseBody().asText()).isEqualTo("POST response: hello");
    }

    @Test
    void testPutMethodCaptured() throws Exception {
        String body = "put content";
        mockMvc.perform(put("/test/http-methods")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("PUT response: put content"));

        HttpRequestEvent requestEvent = eventCaptureListener.getRequestEvents().getFirst();
        HttpResponseEvent responseEvent = eventCaptureListener.getResponseEvents().getFirst();

        assertThat(requestEvent.getMethod()).isEqualTo(HttpMethodEnum.PUT);
        assertThat(requestEvent.getRequestBody().asText()).isEqualTo("put content");

        assertThat(responseEvent.getResponseBody().asText()).isEqualTo("PUT response: put content");
    }

    @Test
    void testPatchMethodCaptured() throws Exception {
        String body = "patching";
        mockMvc.perform(patch("/test/http-methods")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("PATCH response: patching"));

        HttpRequestEvent requestEvent = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(requestEvent.getMethod()).isEqualTo(HttpMethodEnum.PATCH);
    }

    @Test
    void testDeleteMethodCaptured() throws Exception {
        mockMvc.perform(delete("/test/http-methods"))
                .andExpect(status().isNoContent());

        HttpRequestEvent requestEvent = eventCaptureListener.getRequestEvents().getFirst();
        HttpResponseEvent responseEvent = eventCaptureListener.getResponseEvents().getFirst();

        assertThat(requestEvent.getMethod()).isEqualTo(HttpMethodEnum.DELETE);
        assertThat(responseEvent.getResponseStatus()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void testOptionsMethodCaptured() throws Exception {
        mockMvc.perform(options("/test/http-methods"))
                .andExpect(status().isOk());

        HttpRequestEvent requestEvent = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(requestEvent.getMethod()).isEqualTo(HttpMethodEnum.OPTIONS);
    }

    @Test
    void testHeadMethodCaptured() throws Exception {
        mockMvc.perform(head("/test/http-methods"))
                .andExpect(status().isOk())
                .andExpect(content().string("")); // HEAD has no body

        HttpRequestEvent requestEvent = eventCaptureListener.getRequestEvents().getFirst();
        HttpResponseEvent responseEvent = eventCaptureListener.getResponseEvents().getFirst();

        assertThat(requestEvent.getMethod()).isEqualTo(HttpMethodEnum.HEAD);
        assertThat(responseEvent.getResponseBody().isEmpty()).isTrue(); // expected for HEAD
    }

    @Test
    void testUnsupportedMediaTypeReturns415() throws Exception {
        mockMvc.perform(post("/test/http-methods/with-content-type")
                        .contentType("application/unsupported-type")
                        .content("some"))
                .andExpect(status().isUnsupportedMediaType());

        HttpResponseEvent responseEvent = eventCaptureListener.getResponseEvents().getFirst();
        assertThat(responseEvent.getResponseStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void testHeadMethodDoesNotCaptureBody() throws Exception {
        mockMvc.perform(head("/test/http-methods"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        HttpResponseEvent responseEvent = eventCaptureListener.getResponseEvents().getFirst();
        assertThat(responseEvent.getResponseBody().isEmpty()).isTrue(); // HEAD always empty
    }

    @Test
    void testOptionsCapturesAllowHeader() throws Exception {
        mockMvc.perform(options("/test/http-methods"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Allow"));

        HttpResponseEvent responseEvent = eventCaptureListener.getResponseEvents().getFirst();
        assertThat(responseEvent.getResponseHeaders()).containsKey("Allow");
    }

    @Test
    void testSamePathDifferentMethods() throws Exception {
        mockMvc.perform(get("/test/http-methods"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/test/http-methods")
                        .content("post"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/test/http-methods"))
                .andExpect(status().isNoContent());

        List<HttpRequestEvent> requests = eventCaptureListener.getRequestEvents();
        assertThat(requests).hasSize(3);
        assertThat(requests.get(0).getMethod()).isEqualTo(HttpMethodEnum.GET);
        assertThat(requests.get(1).getMethod()).isEqualTo(HttpMethodEnum.POST);
        assertThat(requests.get(2).getMethod()).isEqualTo(HttpMethodEnum.DELETE);
    }

    @Test
    void testEmptyPostBodyHandled() throws Exception {
        mockMvc.perform(post("/test/http-methods")
                        .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk());

        HttpRequestEvent requestEvent = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(requestEvent.getRequestBody().isEmpty()).isTrue();
    }

    @Test
    void testMultipleHeadersWithSameName() throws Exception {
        mockMvc.perform(get("/test/http-methods")
                        .header("X-Test", "one")
                        .header("X-Test", "two"))
                .andExpect(status().isOk());

        HttpRequestEvent requestEvent = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(requestEvent.getHeaders().get("X-Test")).containsExactly("one", "two");
    }


}
