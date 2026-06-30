package com.davidrandoll.spring_web_captor.nativesupport;

import com.davidrandoll.spring_web_captor.WebCaptorXmlApplication;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.setup.EventCaptureListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GraalVM-native-capable boot test for the XML extension. Boots the XML body parsers ENABLED and pushes a real
 * {@code application/xml} body through the captor, asserting the XML-to-JsonNode parsing and the subsequent
 * Jackson serialization of the captured event both survive under a closed-world native image. No Mockito.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorXmlApplication.class)
@AutoConfigureMockMvc
@Tag("native")
class WebCaptorXmlNativeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventCaptureListener eventCaptureListener;

    @BeforeEach
    void setUp() {
        eventCaptureListener.clearEvents();
    }

    @Test
    void parsesXmlBodyAndSerializesEventUnderNative() throws Exception {
        String xml = "<user><name>David</name></user>";
        mockMvc.perform(post("/test/body/xml")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(xml))
                .andExpect(status().isOk());

        HttpRequestEvent request = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(request.getRequestBody().get("name").asText()).isEqualTo("David");

        String json = objectMapper.writeValueAsString(request);
        assertThat(json).contains("David");
        com.fasterxml.jackson.databind.JsonNode tree = objectMapper.readTree(json);
        assertThat(tree.findValue("name").asText()).isEqualTo("David");
    }
}
