package com.davidrandoll.spring_web_captor.multipart;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.setup.EventCaptureListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Tests that multipart file content (base64) is correctly captured in events.
 * Uses a real embedded server because multipart temp file cleanup can cause
 * file.getBytes() to fail if serialization is deferred to JSON rendering time.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MultipartFileContentTest {

    @Autowired
    private EventCaptureListener eventCaptureListener;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        eventCaptureListener.clearEvents();
    }

    private void sendMultipartWithFile(String fileContent, String filename) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(fileContent.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        body.add("description", "test upload");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var response = restTemplate.postForEntity("/test/body/multipart",
                new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(eventCaptureListener.getRequestEvents()).isNotEmpty()
        );
    }

    @Test
    void testMultipartFileBase64ContentIsNotNull() {
        sendMultipartWithFile("hello world", "test.txt");

        HttpRequestEvent event = eventCaptureListener.getRequestEvents().getFirst();

        // Serialize the event to JSON to trigger getSerializedFiles()
        JsonNode json = objectMapper.valueToTree(event);
        JsonNode files = json.path("bodyPayload").path("files").path("file");

        assertThat(files.isArray()).isTrue();
        assertThat(files).hasSize(1);

        JsonNode file = files.get(0);
        assertThat(file.get("filename").asText()).isEqualTo("test.txt");
        assertThat(file.get("contentType").asText()).isEqualTo("text/plain");
        assertThat(file.get("size").asLong()).isEqualTo(11);
        assertThat(file.get("base64Content").isNull())
                .as("base64Content should not be null - file bytes must be captured eagerly before temp cleanup")
                .isFalse();
        assertThat(file.get("base64Content").asText())
                .isEqualTo(Base64.getEncoder().encodeToString("hello world".getBytes()));
    }

    @Test
    void testMultipartFileBase64ContentMatchesOriginal() {
        String content = "The quick brown fox jumps over the lazy dog";
        sendMultipartWithFile(content, "fox.txt");

        HttpRequestEvent event = eventCaptureListener.getRequestEvents().getFirst();
        JsonNode json = objectMapper.valueToTree(event);
        JsonNode file = json.path("bodyPayload").path("files").path("file").get(0);

        String base64 = file.get("base64Content").asText();
        String decoded = new String(Base64.getDecoder().decode(base64));
        assertThat(decoded).isEqualTo(content);
    }
}
