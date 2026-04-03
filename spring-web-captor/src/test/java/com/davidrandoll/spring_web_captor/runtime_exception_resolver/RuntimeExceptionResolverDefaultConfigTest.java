package com.davidrandoll.spring_web_captor.runtime_exception_resolver;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests RuntimeExceptionResolver with default Spring Boot error config (includeMessage=NEVER).
 * Catches Bug 2: the "error" field is gated behind includeMessage instead of being
 * independently controlled. With default config, the response body is missing the "error" field.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RuntimeExceptionResolverDefaultConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testResponseContainsErrorField() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/resolver/runtime-error"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);

        // The "error" field should contain the HTTP status reason phrase.
        // This is a benign, non-sensitive value ("Internal Server Error") that should
        // be present regardless of the includeMessage setting.
        assertThat(json.has("error"))
                .as("Response body should contain 'error' field with HTTP status reason phrase")
                .isTrue();
        assertThat(json.get("error").asText()).isEqualTo("Internal Server Error");
    }

    @Test
    void testResponseContainsTimestampAndStatus() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/resolver/runtime-error"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);

        // These fields should always be present regardless of config
        assertThat(json.has("timestamp")).isTrue();
        assertThat(json.has("status")).isTrue();
        assertThat(json.get("status").asInt()).isEqualTo(500);
    }
}
