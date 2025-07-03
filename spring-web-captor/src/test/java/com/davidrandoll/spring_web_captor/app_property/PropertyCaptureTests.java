package com.davidrandoll.spring_web_captor.app_property;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        "web-captor.event-details.include-endpoint-exists=true",
        "web-captor.event-details.include-full-url=true",
        "web-captor.event-details.include-path=true",
        "web-captor.event-details.include-method=true",
        "web-captor.event-details.include-request-headers=true",
        "web-captor.event-details.include-query-params=true",
        "web-captor.event-details.include-path-params=true",
        "web-captor.event-details.include-response-headers=true",
        "web-captor.event-details.include-response-status=true",
        "web-captor.event-details.include-error-details=true"
})
class PropertyCaptureTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EventCaptureListener eventCaptureListener;

    @BeforeEach
    void setup() {
        eventCaptureListener.clearEvents();
    }

    @Test
    void testAllCapturesPresentWhenEnabled() throws Exception {
        mockMvc.perform(post("/test/property/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"value\"}"))
                .andExpect(status().isOk());

        HttpRequestEvent request = eventCaptureListener.getRequestEvents().getFirst();
        HttpResponseEvent response = eventCaptureListener.getResponseEvents().getFirst();

        assertTrue(request.isEndpointExists());
        assertNotNull(request.getFullUrl());
        assertNotNull(request.getPath());
        assertNotNull(request.getMethod());
        assertNotNull(request.getHeaders());
        assertNotNull(request.getQueryParams());
        assertNotNull(request.getPathParams());

        assertNotNull(response.getResponseHeaders());
        assertNotNull(response.getResponseStatus());
        assertNull(response.getErrorDetail());
    }
}
