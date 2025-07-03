package com.davidrandoll.spring_web_captor.path_variable;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
import com.davidrandoll.spring_web_captor.setup.EventCaptureListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PathVariableTestControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventCaptureListener eventCaptureListener;

    @BeforeEach
    void setUp() {
        eventCaptureListener.clearEvents(); // Reset events before each test
    }

    @Test
    void testBasicPathVariable() throws Exception {
        mockMvc.perform(get("/test/path/basic/123"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("123", request.getPathParams().get("id"));
    }

    @Test
    void testMultiplePathVariables() throws Exception {
        mockMvc.perform(get("/test/path/multi/u42/orders/o99"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("u42", request.getPathParams().get("userId"));
        assertEquals("o99", request.getPathParams().get("orderId"));
    }

    @Test
    void testEncodedPathVariable() throws Exception {
        mockMvc.perform(get("/test/path/encoded/John Doe"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("John Doe", request.getPathParams().get("name")); // should be URL-decoded
    }

    @Test
    void testPathVariableWithSpecialCharacters() throws Exception {
        mockMvc.perform(get("/test/path/encoded/a.b-c_d~e!f$g"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("a.b-c_d~e!f$g", request.getPathParams().get("name"));
    }

    @Test
    void testPathVariableWithUrlLookingValue() throws Exception {
        mockMvc.perform(get("/test/path/basic/http%3A%2F%2Fexample.com"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("http%3A%2F%2Fexample.com", request.getPathParams().get("id"));

        var decodedValue = URLDecoder.decode(request.getPathParams().get("id"), StandardCharsets.UTF_8);
        assertEquals("http://example.com", decodedValue);
    }

    @Test
    void testPathVariableWithUnicodeCharacter() throws Exception {
        mockMvc.perform(get("/test/path/basic/%E2%9C%93")) // ✓
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("%E2%9C%93", request.getPathParams().get("id"));
    }

    @Test
    void testPathVariableWithReservedCharacters() throws Exception {
        mockMvc.perform(get("/test/path/basic/foo%2Fbar")) // encoded slash
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("foo%2Fbar", request.getPathParams().get("id")); // Keep as-is

        String decoded = URLDecoder.decode(request.getPathParams().get("id"), StandardCharsets.UTF_8);
        assertEquals("foo/bar", decoded);
    }

    @Test
    void testLongPathVariable() throws Exception {
        String longId = "x".repeat(1000);
        mockMvc.perform(get("/test/path/basic/" + longId))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals(longId, request.getPathParams().get("id"));
    }

}
