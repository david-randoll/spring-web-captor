package com.davidrandoll.spring_web_captor.content_type;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.setup.EventCaptureListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContentTypeTestControllerTest {
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
    void testJsonContentTypeCaptured() throws Exception {
        String json = "{\"name\":\"David\",\"age\":30}";

        mockMvc.perform(post("/test/content-type/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received.name").value("David"));

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        HttpResponseEvent res = eventCaptureListener.getResponseEvents().getFirst();

        assertThat(req.getRequestBody().get("name").asText()).isEqualTo("David");
        assertThat(req.getHeaders().getContentType().toString()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(res.getResponseBody().get("received").get("name").asText()).isEqualTo("David");
    }

    @Test
    void testXmlContentTypeCaptured() throws Exception {
        String xml = "<person><name>Ishwari</name></person>";

        mockMvc.perform(post("/test/content-type/xml")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(xml))
                .andExpect(status().isOk())
                .andExpect(content().string("<response>" + xml + "</response>"));

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        HttpResponseEvent res = eventCaptureListener.getResponseEvents().getFirst();

        assertThat(req.getRequestBody().asText()).contains("<name>Ishwari</name>");
        assertThat(req.getHeaders().getContentType().toString()).startsWith(MediaType.APPLICATION_XML_VALUE);
        assertThat(res.getResponseBody().asText()).contains("<response>");
    }

    @Test
    void testFormContentTypeCaptured() throws Exception {
        mockMvc.perform(post("/test/content-type/form")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "test@example.com")
                        .param("code", "123"))
                .andExpect(status().isOk())
                .andExpect(content().string("Form received: {email=test@example.com, code=123}"));

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getQueryParams()).containsEntry("email", List.of("test@example.com"));
        assertThat(req.getQueryParams()).containsEntry("code", List.of("123"));
    }

    @Test
    void testPlainTextContentTypeCaptured() throws Exception {
        String text = "hello plain world";

        mockMvc.perform(post("/test/content-type/text")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(text))
                .andExpect(status().isOk())
                .andExpect(content().string("Text received: " + text));

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        HttpResponseEvent res = eventCaptureListener.getResponseEvents().getFirst();

        assertThat(req.getRequestBody().asText()).isEqualTo(text);
        assertThat(req.getHeaders().getContentType().toString()).startsWith(MediaType.TEXT_PLAIN_VALUE);
        assertThat(res.getResponseBody().asText()).contains("Text received");
    }

    @Test
    void testInvalidJsonStillCaptured() throws Exception {
        mockMvc.perform(post("/test/content-type/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-a-json"))
                .andExpect(status().isBadRequest());

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        HttpResponseEvent res = eventCaptureListener.getResponseEvents().getFirst();

        assertThat(req.getHeaders().getContentType().toString()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(res.getResponseStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        // check that the request body is "not-a-json"
        assertThat(req.getRequestBody().asText()).isEqualTo("not-a-json");
    }
}