package com.davidrandoll.spring_web_captor.app_property.response_status;

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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "web-captor.event-details.include-response-status=false"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResponseStatusExcludedTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EventCaptureListener eventCaptureListener;

    @BeforeEach
    void setup() {
        eventCaptureListener.clearEvents();
    }

    @Test
    void testResponseStatusNotCapturedWhenDisabled() throws Exception {
        int statusCode = 404;
        mockMvc.perform(get("/test/property/response-status/" + statusCode))
                .andExpect(status().isNotFound());

        var responseEvent = eventCaptureListener.getResponseEvents().getFirst();
        var responseStatus = responseEvent.getResponseStatus();

        assertNull(responseStatus, "Response status should not be captured");
    }
}