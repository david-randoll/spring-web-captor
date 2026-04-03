package com.davidrandoll.spring_web_captor.runtime_exception_resolver;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests RuntimeExceptionResolver with includeMessage=ALWAYS.
 * When both conditions are true, the copy-paste bug is hidden -- both "error" and "message"
 * appear in the response. This test verifies the "happy path" still works.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {"server.error.include-message=ALWAYS"})
class RuntimeExceptionResolverMessageAlwaysTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testResponseContainsBothErrorAndMessage() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/resolver/runtime-error"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);

        assertThat(json.has("error")).isTrue();
        assertThat(json.get("error").asText()).isEqualTo("Internal Server Error");

        assertThat(json.has("message")).isTrue();
        assertThat(json.get("message").asText()).isEqualTo("Test runtime error");
    }
}
