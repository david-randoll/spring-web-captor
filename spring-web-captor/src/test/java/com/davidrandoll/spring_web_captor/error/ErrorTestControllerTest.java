package com.davidrandoll.spring_web_captor.error;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.setup.EventCaptureListener;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ErrorTestControllerTest {
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
    void testRuntimeException() throws Exception {
        mockMvc.perform(get("/test/error/runtime"))
                .andExpect(status().isInternalServerError());

        assertErrorCaptured("Simulated runtime exception", 500);
    }

    @Test
    void testNullPointerException() throws Exception {
        mockMvc.perform(get("/test/error/nullpointer"))
                .andExpect(status().isInternalServerError());

        assertErrorCaptured("NullPointerException", 500);
    }

    @Test
    void testCustomTeapotException() throws Exception {
        mockMvc.perform(get("/test/error/custom"))
                .andExpect(status().isIAmATeapot());

        assertErrorCaptured("I'm a teapot", 418);
    }

    @Test
    void testNotFoundException() throws Exception {
        mockMvc.perform(get("/test/error/notfound"))
                .andExpect(status().isNotFound());

        assertErrorCaptured("Resource not found", 404);
    }

    @Test
    void testValidationException() throws Exception {
        String invalidJson = "{}"; // name is missing

        mockMvc.perform(post("/test/error/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        assertErrorCaptured("name", 400);
    }

    @Test
    void testValidationError() throws Exception {
        mockMvc.perform(post("/test/error/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ }")) // missing required `name`
                .andExpect(status().isBadRequest());

        assertErrorCaptured("Field error in object 'dummyRequest' on field 'name'", 400);
    }

    @Test
    void testMalformedJson() throws Exception {
        mockMvc.perform(post("/test/error/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ name: 'incomplete' ")) // bad JSON
                .andExpect(status().isBadRequest());

        assertErrorCaptured("JSON parse error", 400);
    }

    @Test
    void testMissingContentTypeWithJson() throws Exception {
        mockMvc.perform(post("/test/error/validation")
                        .content("{ \"name\": \"test\" }")) // no content type
                .andExpect(status().isUnsupportedMediaType());

        assertErrorCaptured("HttpMediaTypeNotSupportedException", 415);
    }

    @Test
    void testUnsupportedMethod() throws Exception {
        mockMvc.perform(post("/test/error/runtime")) // runtime supports only GET
                .andExpect(status().isMethodNotAllowed());

        assertErrorCaptured("Request method 'POST' is not supported", 405);
    }

    @Test
    void testErrorDetailStatusMatchesResponseStatus() throws Exception {
        mockMvc.perform(get("/test/error/runtime"))
                .andExpect(status().isInternalServerError());

        HttpResponseEvent event = eventCaptureListener.getResponseEvents().getFirst();
        assertEquals(500, event.getErrorDetail().get("status"),
                "errorDetail.status should match the actual HTTP status, not default to 999");
    }

    @Test
    void testErrorDetailStatusMatchesFor404() throws Exception {
        mockMvc.perform(get("/test/error/notfound"))
                .andExpect(status().isNotFound());

        HttpResponseEvent event = eventCaptureListener.getResponseEvents().getFirst();
        assertEquals(404, event.getErrorDetail().get("status"),
                "errorDetail.status should be 404 for not found errors");
    }

    @Test
    void testErrorDetailStatusMatchesFor418() throws Exception {
        mockMvc.perform(get("/test/error/custom"))
                .andExpect(status().isIAmATeapot());

        HttpResponseEvent event = eventCaptureListener.getResponseEvents().getFirst();
        assertEquals(418, event.getErrorDetail().get("status"),
                "errorDetail.status should be 418 for teapot errors");
    }

    @Test
    void testIsServerErrorResponse_forRuntimeException() throws Exception {
        mockMvc.perform(get("/test/error/runtime"))
                .andExpect(status().isInternalServerError());

        HttpResponseEvent event = eventCaptureListener.getResponseEvents().getFirst();
        assertThat(event.isServerErrorResponse()).isTrue();
        assertThat(event.isErrorResponse()).isTrue();
        assertThat(event.isSuccessResponse()).isFalse();
        assertThat(event.isClientErrorResponse()).isFalse();
    }

    @Test
    void testIsClientErrorResponse_for404() throws Exception {
        mockMvc.perform(get("/test/error/notfound"))
                .andExpect(status().isNotFound());

        HttpResponseEvent event = eventCaptureListener.getResponseEvents().getFirst();
        assertThat(event.isClientErrorResponse()).isTrue();
        assertThat(event.isErrorResponse()).isTrue();
        assertThat(event.isSuccessResponse()).isFalse();
        assertThat(event.isServerErrorResponse()).isFalse();
    }

    private void assertErrorCaptured(String expectedMessageFragment, int expectedStatus) throws JsonProcessingException {
        HttpResponseEvent event = eventCaptureListener.getResponseEvents().stream()
                .filter(e -> e.getResponseStatus().value() == expectedStatus)
                .findFirst()
                .orElseThrow();

        String details = objectMapper.writeValueAsString(event.getErrorDetail());
        assertTrue(details.contains(expectedMessageFragment), "Expected error message to contain: " + expectedMessageFragment);
    }
}
