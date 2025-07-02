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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)

@TestPropertySource(properties = {
        "web-captor.event-details.include-endpoint-exists=false",
        "web-captor.event-details.include-full-url=false",
        "web-captor.event-details.include-path=false",
        "web-captor.event-details.include-method=false",
        "web-captor.event-details.include-request-headers=false",
        "web-captor.event-details.include-query-params=false",
        "web-captor.event-details.include-path-params=false",
        "web-captor.event-details.include-response-headers=false",
        "web-captor.event-details.include-response-status=false",
        "web-captor.event-details.include-error-details=false"
})
class PropertyCaptureDisabledTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EventCaptureListener eventCaptureListener;

    @BeforeEach
    void setup() {
        eventCaptureListener.clearEvents();
    }

    @Test
    void testAllCapturesNotPresentWhenDisabled() throws Exception {
        mockMvc.perform(post("/test/property/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"value\"}"))
                .andExpect(status().isOk());

        HttpRequestEvent request = eventCaptureListener.getRequestEvents().getFirst();
        HttpResponseEvent response = eventCaptureListener.getResponseEvents().getFirst();

        assertFalse(request.isEndpointExists());
        assertNull(request.getFullUrl());
        assertNull(request.getPath());
        assertNull(request.getMethod());
        assertNull(request.getHeaders());
        assertNull(request.getQueryParams());
        assertNull(request.getPathParams());

        assertNull(response.getResponseHeaders());
        assertNull(response.getResponseStatus());
        assertNull(response.getErrorDetail());
    }
}
