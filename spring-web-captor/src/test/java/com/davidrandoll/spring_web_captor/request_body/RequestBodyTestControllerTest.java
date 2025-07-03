package com.davidrandoll.spring_web_captor.request_body;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
import com.davidrandoll.spring_web_captor.setup.EventCaptureListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequestBodyTestControllerTest {
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
    void testJsonBody() throws Exception {
        var payload = Map.of("name", "David", "age", 30);
        mockMvc.perform(post("/test/body/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("David", request.getRequestBody().get("name").asText());
    }

    @Test
    void testPlainTextBody() throws Exception {
        mockMvc.perform(post("/test/body/text")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Hello world"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("Hello world", request.getRequestBody().asText());
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

    @Test
    void testEmptyBody() throws Exception {
        mockMvc.perform(post("/test/body/empty"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertTrue(request.getRequestBody().isNull() || request.getRequestBody().isMissingNode());
    }

    @Test
    void testMalformedJson() throws Exception {
        mockMvc.perform(post("/test/body/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json"))
                .andExpect(status().isBadRequest()); // Spring should reject

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertTrue(request.getRequestBody().isTextual());
        assertTrue(request.getRequestBody().asText().contains("invalid"));
    }

    @Test
    void testUtf8EncodedJson() throws Exception {
        String utf8Json = objectMapper.writeValueAsString(Map.of("emoji", "😃"));
        mockMvc.perform(post("/test/body/json")
                        .contentType("application/json; charset=UTF-8")
                        .content(utf8Json))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("😃", request.getRequestBody().get("emoji").asText());
    }

    @Test
    void testFormUrlEncodedBody() throws Exception {
        mockMvc.perform(post("/test/body/form")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("name=David&email=test%40example.com"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("David", request.getRequestBody().get("name").asText());
    }

    @Test
    void testWhitespaceOnlyBody() throws Exception {
        mockMvc.perform(post("/test/body/text")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(" \n\t "))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals(" \n\t ", request.getRequestBody().asText());
    }

    @Test
    void testJsonSpecialCharacters() throws Exception {
        var payload = Map.of("quote", "\"Hello\"", "backslash", "C:\\Path");
        mockMvc.perform(post("/test/body/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("C:\\Path", request.getRequestBody().get("backslash").asText());
    }

    @Test
    void testUnsupportedContentType() throws Exception {
        mockMvc.perform(post("/test/body/text")
                        .contentType("application/ndjson")
                        .content("{ \"item\": 1 }\n{ \"item\": 2 }"))
                .andExpect(status().isUnsupportedMediaType()); // Or handled based on controller
    }

    @Test
    void testNullJsonLiteral() throws Exception {
        mockMvc.perform(post("/test/body/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest()); // or OK depending on deserializer

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertTrue(request.getRequestBody().isNull());
    }

    @Test
    void testFormUrlEncoded() throws Exception {
        mockMvc.perform(post("/test/body/form")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("username=test&password=1234"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("test", request.getRequestBody().get("username").asText());
    }

    @Test
    void testMultipart() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE, "hello world".getBytes());
        mockMvc.perform(multipart("/test/body/multipart")
                        .file(file)
                        .param("description", "Test file"))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        assertEquals("Test file", request.getRequestBody().get("description").asText());
        MultipartFile fileResult = request.getRequestFiles().getFirst("file");
        assertNotNull(fileResult);
        assertEquals("hello.txt", fileResult.getOriginalFilename());
        assertFalse(fileResult.isEmpty(), "Expected file to not be empty");
        assertArrayEquals("hello world".getBytes(), fileResult.getBytes(), "Expected file content to match");
    }

    @Test
    void testMultipartWithEmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", MediaType.TEXT_PLAIN_VALUE, new byte[0]);
        mockMvc.perform(multipart("/test/body/multipart")
                        .file(emptyFile))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        MultipartFile file = request.getRequestFiles().getFirst("file");
        assertNotNull(file);
        assertEquals("empty.txt", file.getOriginalFilename());
        assertTrue(file.isEmpty(), "Expected file to be empty");
        assertArrayEquals(new byte[0], file.getBytes(), "Expected file content to be empty");
    }

    @Test
    void testMultipartMissingFileParam() throws Exception {
        mockMvc.perform(multipart("/test/body/multipart")
                        .param("description", "Missing file"))
                .andExpect(status().isBadRequest()); // Expect controller to fail
    }

}