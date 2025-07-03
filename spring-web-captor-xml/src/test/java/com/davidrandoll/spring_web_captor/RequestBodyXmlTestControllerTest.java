package com.davidrandoll.spring_web_captor;

import com.davidrandoll.spring_web_captor.setup.EventCaptureListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorXmlApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequestBodyXmlTestControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventCaptureListener eventCaptureListener;

    @BeforeEach
    void setUp() {
        eventCaptureListener.clearEvents(); // Reset events before each test
    }

    @Test
    void testXmlBody() throws Exception {
        String xml = "<user><name>David</name></user>";
        mockMvc.perform(post("/test/body/xml")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(xml))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals(xml, request.getRequestBody().asText());
    }
}