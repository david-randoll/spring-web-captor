package com.davidrandoll.spring_web_captor.app_property.response_headers;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
import com.davidrandoll.spring_web_captor.setup.EventCaptureListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "web-captor.event-details.include-response-headers=true"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResponseHeadersIncludedTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EventCaptureListener eventCaptureListener;

    @BeforeEach
    void setup() {
        eventCaptureListener.clearEvents();
    }

    @Test
    void testResponseHeadersCapturedWhenEnabled() throws Exception {
        mockMvc.perform(get("/test/property/response-headers"))
                .andExpect(status().isOk());

        var responseEvent = eventCaptureListener.getResponseEvents().getFirst();
        var responseHeaders = responseEvent.getResponseHeaders();

        assertNotNull(responseHeaders);
        assertEquals("CustomValue", responseHeaders.getFirst("X-Custom-Header"));
    }
}