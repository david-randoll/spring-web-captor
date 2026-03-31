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

    @Test
    void testInfinitePathVariables_capturesKeyOnly() throws Exception {
        mockMvc.perform(get("/test/path/myKey"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("myKey", request.getPathParams().get("key"));
    }

    @Test
    void testInfinitePathVariables_capturesKeyAndOneExtra() throws Exception {
        mockMvc.perform(get("/test/path/category/electronics"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("category", request.getPathParams().get("key"));
        assertEquals("electronics", request.getPathParams().get("path1"));
    }

    @Test
    void testInfinitePathVariables_capturesKeyAndTwoExtra() throws Exception {
        mockMvc.perform(get("/test/path/settings/theme/dark"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("settings", request.getPathParams().get("key"));
        assertEquals("theme", request.getPathParams().get("path1"));
        assertEquals("dark", request.getPathParams().get("path2"));
    }

    @Test
    void testInfinitePathVariables_capturesKeyAndDeeplyNestedSegments() throws Exception {
        mockMvc.perform(get("/test/path/region/us/east/zone1/rack42"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("region", request.getPathParams().get("key"));
        assertEquals("us", request.getPathParams().get("path1"));
        assertEquals("east", request.getPathParams().get("path2"));
        assertEquals("zone1", request.getPathParams().get("path3"));
        assertEquals("rack42", request.getPathParams().get("path4"));
    }

    @Test
    void testInfinitePathVariables_capturesNumericSegments() throws Exception {
        mockMvc.perform(get("/test/path/item/42/variant/7"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("item", request.getPathParams().get("key"));
        assertEquals("42", request.getPathParams().get("path1"));
        assertEquals("variant", request.getPathParams().get("path2"));
        assertEquals("7", request.getPathParams().get("path3"));
    }

    @Test
    void testInfinitePathVariables_capturesSpecialCharSegments() throws Exception {
        mockMvc.perform(get("/test/path/my-config_v2/sub-path/value.json"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("my-config_v2", request.getPathParams().get("key"));
        assertEquals("sub-path", request.getPathParams().get("path1"));
        assertEquals("value.json", request.getPathParams().get("path2"));
    }

    // --- infinitePathVariablesWithEnd: /with-end/{key}/{*rest} ---

    @Test
    void testInfinitePathVariablesWithEnd_capturesRestSegments() throws Exception {
        mockMvc.perform(get("/test/path/with-end/config/a/b/c"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("config", request.getPathParams().get("key"));
        assertEquals("a", request.getPathParams().get("path1"));
        assertEquals("b", request.getPathParams().get("path2"));
        assertEquals("c", request.getPathParams().get("path3"));
    }

    @Test
    void testInfinitePathVariablesWithEnd_capturesSingleRestSegment() throws Exception {
        mockMvc.perform(get("/test/path/with-end/config/only"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("config", request.getPathParams().get("key"));
        assertEquals("only", request.getPathParams().get("path1"));
    }

    @Test
    void testInfinitePathVariablesWithEnd_capturesDeeplyNested() throws Exception {
        mockMvc.perform(get("/test/path/with-end/config/x/y/z/w"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("config", request.getPathParams().get("key"));
        assertEquals("x", request.getPathParams().get("path1"));
        assertEquals("y", request.getPathParams().get("path2"));
        assertEquals("z", request.getPathParams().get("path3"));
        assertEquals("w", request.getPathParams().get("path4"));
    }

    // --- infinitePathVariablesWithMiddleAndPathAgain: /with-middle/{key}/{*rest} ---

    @Test
    void testInfinitePathVariablesWithMiddle_capturesRestSegments() throws Exception {
        mockMvc.perform(get("/test/path/with-middle/root/a/b/c/d"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("root", request.getPathParams().get("key"));
        assertEquals("a", request.getPathParams().get("path1"));
        assertEquals("b", request.getPathParams().get("path2"));
        assertEquals("c", request.getPathParams().get("path3"));
        assertEquals("d", request.getPathParams().get("path4"));
    }

    @Test
    void testInfinitePathVariablesWithMiddle_capturesSingleRestSegment() throws Exception {
        mockMvc.perform(get("/test/path/with-middle/root/x"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("root", request.getPathParams().get("key"));
        assertEquals("x", request.getPathParams().get("path1"));
    }

    @Test
    void testInfinitePathVariablesWithMiddle_capturesDeeplyNested() throws Exception {
        mockMvc.perform(get("/test/path/with-middle/root/a/b/c/d/e/f"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("root", request.getPathParams().get("key"));
        assertEquals("a", request.getPathParams().get("path1"));
        assertEquals("b", request.getPathParams().get("path2"));
        assertEquals("c", request.getPathParams().get("path3"));
        assertEquals("d", request.getPathParams().get("path4"));
        assertEquals("e", request.getPathParams().get("path5"));
        assertEquals("f", request.getPathParams().get("path6"));
    }

}
