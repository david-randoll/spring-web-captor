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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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

    @Test
    void testEmptyJsonBodyStillParses() throws Exception {
        mockMvc.perform(post("/test/content-type/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest()); // or your app’s behavior

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getHeaders().getContentType().toString()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(req.getRequestBody().toString()).isEqualTo("null");
    }

    @Test
    void testContentTypeMismatchStillCapturesEvent() throws Exception {
        String xml = "<person><name>Wrong</name></person>";

        mockMvc.perform(post("/test/content-type/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(xml))
                .andExpect(status().isBadRequest());

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        HttpResponseEvent res = eventCaptureListener.getResponseEvents().getFirst();

        assertThat(req.getRequestBody().asText()).contains("<name>");
        assertThat(res.getResponseStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testNoContentTypeHeader() throws Exception {
        String body = "{\"free\":\"form\"}";

        mockMvc.perform(post("/test/content-type/json")
                        .content(body)) // No content-type!
                .andExpect(status().isUnsupportedMediaType()); // or 400/415 depending on controller config

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getRequestBody().asText()).contains("free");
        assertThat(req.getHeaders().getContentType()).isNull();
    }

    @Test
    void testMultipleContentTypeHeaders() throws Exception {
        mockMvc.perform(post("/test/content-type/text")
                        .content("some data")
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .header(HttpHeaders.CONTENT_TYPE, "text/plain"))// duplicate header
                .andExpect(status().isUnsupportedMediaType()); // likely behavior

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getHeaders().get("Content-Type").getFirst()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(req.getRequestBody().asText()).isEqualTo("some data");
    }

    @Test
    void testLargeJsonPayload() throws Exception {
        StringBuilder largeJson = new StringBuilder("{\"data\":\"");
        largeJson.append("x".repeat(500_000)); // 500 KB
        largeJson.append("\"}");

        mockMvc.perform(post("/test/content-type/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(largeJson.toString()))
                .andExpect(status().isOk());

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getRequestBody().get("data").asText()).startsWith("x");
    }

    @Test
    void testUtf16TextContent() throws Exception {
        String text = "unicode-test";
        byte[] utf16Bytes = text.getBytes(StandardCharsets.UTF_16);

        mockMvc.perform(post("/test/content-type/text")
                        .contentType("text/plain;charset=UTF-16")
                        .content(utf16Bytes))
                .andExpect(status().isOk());

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getRequestBody().asText()).contains("unicode-test");
    }

    @Test
    void testMultipartFormUpload() throws Exception {
        mockMvc.perform(multipart("/test/content-type/upload")
                        .file("file", "sample data".getBytes())
                        .param("description", "test file"))
                .andExpect(status().isOk());

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getHeaders().getContentType().toString()).startsWith("multipart/");
        assertThat(req.getRequestBody().get("description").asText()).isEqualTo("test file");

        var file = req.getRequestFiles().getFirst("file");
        assertThat(file).isNotNull();
        assertThat(file.getName()).isEqualTo("file");
        // check the file content
        assertThat(file.getBytes()).isEqualTo("sample data".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void testInvalidUtf8Body() throws Exception {
        byte[] invalidUtf8 = {(byte) 0xC3, (byte) 0x28}; // Invalid 2-byte sequence

        mockMvc.perform(post("/test/content-type/json")
                        .contentType("application/json; charset=UTF-8")
                        .content(invalidUtf8))
                .andExpect(status().isBadRequest());

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("�(", req.getRequestBody().asText());
    }

    @Test
    void testContentTypeWithParameters() throws Exception {
        String body = """
                {"message": "Hello with profile"}
                """;

        mockMvc.perform(post("/test/content-type/params")
                        .contentType("application/json; profile=\"https://example.com/schema\"; charset=UTF-8")
                        .content(body))
                .andExpect(status().isOk());

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();

        var contentType = req.getHeaders().getContentType();
        // Assert full header string captured
        assertThat(contentType).isNotNull();
        assertThat(contentType.toString()).contains("application/json");
        assertThat(contentType.toString()).contains("profile");
        assertThat(req.getRequestBody().toString()).contains("Hello with profile");
    }

    @Test
    void testMalformedContentTypeTrailingSemicolon() throws Exception {
        String body = """
                {"note": "trailing semicolon"}
                """;

        mockMvc.perform(post("/test/content-type/malformed")
                        .header("Content-Type", "application/json;") // manually bypass contentType()
                        .content(body))
                .andExpect(status().isOk());

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();

        var contentType = req.getHeaders().getContentType().toString();
        assertThat(contentType).contains("application/json;");
        assertThat(req.getRequestBody().toString()).contains("trailing semicolon");
    }
}