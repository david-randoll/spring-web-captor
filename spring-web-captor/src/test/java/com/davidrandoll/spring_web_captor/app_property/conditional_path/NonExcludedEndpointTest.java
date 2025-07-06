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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "web-captor.excluded-endpoints[0].method=POST",
        "web-captor.excluded-endpoints[0].path=/test/excluded"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NonExcludedEndpointTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EventCaptureListener eventCaptureListener;

    @BeforeEach
    void setup() {
        eventCaptureListener.clearEvents();
    }


    @Test
    void requestToUnexcludedEndpointShouldPublishEvent() throws Exception {
        mockMvc.perform(get("/test/excluded"))
                .andExpect(status().isOk());

        assertFalse(eventCaptureListener.getRequestEvents().isEmpty(), "Request event should be published");
        assertFalse(eventCaptureListener.getResponseEvents().isEmpty(), "Response event should be published");
    }
}
