package com.davidrandoll.spring_web_captor.response_body;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
import com.davidrandoll.spring_web_captor.setup.EventCaptureListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResponseBodyTestControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EventCaptureListener eventCaptureListener;

    @BeforeEach
    void setUp() {
        eventCaptureListener.clearEvents();
    }

    @Test
    void testJsonResponse() throws Exception {
        mockMvc.perform(get("/test/response/json"))
                .andExpect(status().isOk());

        var response = eventCaptureListener.getResponseEvents().getFirst();
        assertEquals("Hello", response.getResponseBody().get("message").asText());
        assertEquals(200, response.getResponseBody().get("status").asInt());
    }

    @Test
    void testTextResponse() throws Exception {
        mockMvc.perform(get("/test/response/text"))
                .andExpect(status().isOk());

        var response = eventCaptureListener.getResponseEvents().getFirst();
        assertEquals("Plain text response", response.getResponseBody().asText());
    }

    @Test
    void testXmlResponse() throws Exception {
        mockMvc.perform(get("/test/response/xml"))
                .andExpect(status().isOk());

        var response = eventCaptureListener.getResponseEvents().getFirst();
        assertEquals("<response><message>Hello</message></response>", response.getResponseBody().asText());
    }

    @Test
    void testEmptyResponse() throws Exception {
        mockMvc.perform(get("/test/response/empty"))
                .andExpect(status().isNoContent());

        var response = eventCaptureListener.getResponseEvents().getFirst();
        assertTrue(response.getResponseBody().isNull() || response.getResponseBody().isMissingNode());
    }

    @Test
    void testNullResponse() throws Exception {
        mockMvc.perform(get("/test/response/null"))
                .andExpect(status().isOk());

        var response = eventCaptureListener.getResponseEvents().getFirst();
        assertTrue(response.getResponseBody().isNull() || response.getResponseBody().isMissingNode());
    }

    @Test
    void testUtf8Response() throws Exception {
        mockMvc.perform(get("/test/response/utf8"))
                .andExpect(status().isOk());

        var response = eventCaptureListener.getResponseEvents().getFirst();
        assertEquals("Emoji 😃 and Unicode ✓", response.getResponseBody().asText());
    }

    @Test
    void testSpecialCharsResponse() throws Exception {
        mockMvc.perform(get("/test/response/special-chars"))
                .andExpect(status().isOk());

        var response = eventCaptureListener.getResponseEvents().getFirst();
        assertEquals("Quotes: \" and Backslashes: \\", response.getResponseBody().asText());
    }
}
