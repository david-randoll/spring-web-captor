package com.davidrandoll.spring_web_captor.app_property.path;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "web-captor.event-details.include-path=false"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PathExcludedTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EventCaptureListener eventCaptureListener;

    @BeforeEach
    void setup() {
        eventCaptureListener.clearEvents();
    }

    @Test
    void testPathIsNullWhenDisabled() throws Exception {
        mockMvc.perform(post("/test/property/echo")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertNull(request.getPath(), "Expected path to be null when disabled");
    }
}