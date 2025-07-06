package com.davidrandoll.spring_web_captor.app_property.conditional_path;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        "web-captor.excluded-endpoints[0].method=GET, POST",
        "web-captor.excluded-endpoints[0].path=/test/filter/multi"
})
class MultiMethodExclusionTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EventCaptureListener eventCaptureListener;

    @BeforeEach
    void setup() {
        eventCaptureListener.clearEvents();
    }

    @Test
    void getShouldBeExcluded() throws Exception {
        mockMvc.perform(get("/test/filter/multi"))
                .andExpect(status().isOk());

        assertTrue(eventCaptureListener.getRequestEvents().isEmpty());
    }

    @Test
    void postShouldBeExcluded() throws Exception {
        mockMvc.perform(post("/test/filter/multi"))
                .andExpect(status().isOk());

        assertTrue(eventCaptureListener.getRequestEvents().isEmpty());
    }

    @Test
    void putShouldNotBeExcluded() throws Exception {
        mockMvc.perform(put("/test/filter/multi"))
                .andExpect(status().isOk());

        assertFalse(eventCaptureListener.getRequestEvents().isEmpty());
    }
}
