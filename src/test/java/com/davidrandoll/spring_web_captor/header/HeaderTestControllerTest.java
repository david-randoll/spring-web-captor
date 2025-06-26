package com.davidrandoll.spring_web_captor.header;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
import com.davidrandoll.spring_web_captor.setup.EventCaptureListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HeaderTestControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventCaptureListener eventCaptureListener; // Capture published events

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        eventCaptureListener.clearEvents(); // Reset events before each test
    }

    @Test
    void testBasicHeaderProvided() throws Exception {
        mockMvc.perform(get("/test/headers/basic")
                        .header("X-Test-Header", "hello"))
                .andExpect(status().isOk());

        var event = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(event.getHeaders().get("x-test-header")).containsExactly("hello");
    }

    @Test
    void testBasicHeaderMissing() throws Exception {
        mockMvc.perform(get("/test/headers/basic"))
                .andExpect(status().isOk());

        var event = eventCaptureListener.getRequestEvents().getFirst();
        assertFalse(event.getHeaders().containsKey("x-test-header"));
    }

    @Test
    void testMultipleValuesForSameHeader() throws Exception {
        mockMvc.perform(get("/test/headers/multi")
                        .header("X-Multi-Header", "val1")
                        .header("X-Multi-Header", "val2"))
                .andExpect(status().isOk());

        var event = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(event.getHeaders().get("x-multi-header")).containsExactly("val1", "val2");
    }

    @Test
    void testRequiredHeaderMissing_shouldFailBeforeEvent() throws Exception {
        mockMvc.perform(get("/test/headers/required"))
                .andExpect(status().isBadRequest());

        var event = eventCaptureListener.getRequestEvents().getFirst();
        assertTrue(event.getHeaders().isEmpty(), "Request event should not be published if required header is missing");
    }

    @Test
    void testCaseInsensitiveHeaderMatch() throws Exception {
        mockMvc.perform(get("/test/headers/case")
                        .header("x-case-header", "VaLuE"))
                .andExpect(status().isOk());

        var event = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(event.getHeaders().get("x-case-header")).containsExactly("VaLuE");
    }

    @Test
    void testHeaderWithEmptyValue() throws Exception {
        mockMvc.perform(get("/test/headers/basic")
                        .header("X-Test-Header", ""))
                .andExpect(status().isOk());

        var event = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(event.getHeaders().get("x-test-header")).containsExactly("");
    }

    @Test
    void testHeaderWithSpecialCharacters() throws Exception {
        mockMvc.perform(get("/test/headers/basic")
                        .header("X-Test-Header", "#$%@!~"))
                .andExpect(status().isOk());

        var event = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(event.getHeaders().get("x-Test-Header")).containsExactly("#$%@!~");
    }

    @Test
    void testMultipleCustomHeaders() throws Exception {
        mockMvc.perform(get("/test/headers/multi")
                        .header("X-One", "one")
                        .header("X-Two", "two"))
                .andExpect(status().isOk());

        var event = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(event.getHeaders().get("X-One")).containsExactly("one");
        assertThat(event.getHeaders().get("X-Two")).containsExactly("two");
    }
}
