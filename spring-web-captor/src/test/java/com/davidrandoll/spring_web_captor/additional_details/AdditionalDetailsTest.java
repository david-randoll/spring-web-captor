package com.davidrandoll.spring_web_captor.additional_details;

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
 * Verifies that the additional detail extensions (duration, IP, user-agent)
 * populate additionalData in captured events.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdditionalDetailsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventCaptureListener eventCaptureListener;

    @BeforeEach
    void setUp() {
        eventCaptureListener.clearEvents();
    }

    @Test
    void testDurationPresentInResponseEvent() throws Exception {
        mockMvc.perform(get("/test/get"))
                .andExpect(status().isOk());

        assertThat(eventCaptureListener.getResponseEvents()).hasSize(1);
        HttpResponseEvent event = eventCaptureListener.getResponseEvents().getFirst();

        assertThat(event.hasAdditionalData("duration")).isTrue();
        assertThat(event.hasAdditionalData("startTime")).isTrue();
        assertThat(event.hasAdditionalData("endTime")).isTrue();
    }

    @Test
    void testIpAddressPresentInRequestEvent() throws Exception {
        mockMvc.perform(get("/test/get"))
                .andExpect(status().isOk());

        assertThat(eventCaptureListener.getRequestEvents()).hasSize(1);
        HttpRequestEvent event = eventCaptureListener.getRequestEvents().getFirst();

        assertThat(event.hasAdditionalData("userIp")).isTrue();
    }

    @Test
    void testIpAddressPresentInResponseEvent() throws Exception {
        mockMvc.perform(get("/test/get"))
                .andExpect(status().isOk());

        assertThat(eventCaptureListener.getResponseEvents()).hasSize(1);
        HttpResponseEvent event = eventCaptureListener.getResponseEvents().getFirst();

        assertThat(event.hasAdditionalData("userIp")).isTrue();
    }

    @Test
    void testUserAgentPresentInRequestEvent() throws Exception {
        mockMvc.perform(get("/test/get")
                        .header("User-Agent", "TestBrowser/1.0"))
                .andExpect(status().isOk());

        assertThat(eventCaptureListener.getRequestEvents()).hasSize(1);
        HttpRequestEvent event = eventCaptureListener.getRequestEvents().getFirst();

        assertThat(event.hasAdditionalData("userAgent")).isTrue();
        assertThat(event.getAdditionalData("userAgent", String.class)).isEqualTo("TestBrowser/1.0");
    }

    @Test
    void testUserAgentPresentInResponseEvent() throws Exception {
        mockMvc.perform(get("/test/get")
                        .header("User-Agent", "TestBrowser/1.0"))
                .andExpect(status().isOk());

        assertThat(eventCaptureListener.getResponseEvents()).hasSize(1);
        HttpResponseEvent event = eventCaptureListener.getResponseEvents().getFirst();

        assertThat(event.hasAdditionalData("userAgent")).isTrue();
        assertThat(event.getAdditionalData("userAgent", String.class)).isEqualTo("TestBrowser/1.0");
    }
}
