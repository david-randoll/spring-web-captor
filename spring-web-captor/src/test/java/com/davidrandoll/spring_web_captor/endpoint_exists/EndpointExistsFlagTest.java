package com.davidrandoll.spring_web_captor.endpoint_exists;

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
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that endpointExists is true for mapped endpoints and false for 404s.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EndpointExistsFlagTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventCaptureListener eventCaptureListener;

    @BeforeEach
    void setUp() {
        eventCaptureListener.clearEvents();
    }

    @Test
    void testEndpointExistsTrue_forRealEndpoint() throws Exception {
        mockMvc.perform(get("/test/get"))
                .andExpect(status().isOk());

        assertThat(eventCaptureListener.getRequestEvents()).hasSize(1);
        assertThat(eventCaptureListener.getResponseEvents()).hasSize(1);

        HttpRequestEvent requestEvent = eventCaptureListener.getRequestEvents().getFirst();
        HttpResponseEvent responseEvent = eventCaptureListener.getResponseEvents().getFirst();

        assertThat(requestEvent.isEndpointExists()).isTrue();
        assertThat(responseEvent.isEndpointExists()).isTrue();
    }

    @Test
    void testEndpointExistsFalse_for404() throws Exception {
        mockMvc.perform(get("/completely-nonexistent-path"))
                .andExpect(status().isNotFound());

        assertThat(eventCaptureListener.getRequestEvents()).hasSize(1);
        assertThat(eventCaptureListener.getResponseEvents()).hasSize(1);

        HttpRequestEvent requestEvent = eventCaptureListener.getRequestEvents().getFirst();
        HttpResponseEvent responseEvent = eventCaptureListener.getResponseEvents().getFirst();

        assertThat(requestEvent.isEndpointExists()).isFalse();
        assertThat(responseEvent.isEndpointExists()).isFalse();
    }
}
