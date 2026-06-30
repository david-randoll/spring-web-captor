package com.davidrandoll.spring_web_captor.nativesupport;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.setup.EventCaptureListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GraalVM-native-capable boot test for the core captor. Boots the captor ENABLED, drives a real request
 * through the filter chain, and round-trips the captured events through Jackson. The event/DTO graph is what
 * consuming services serialize (into a workflow context, an audit row, a message body); under a closed-world
 * native image that serialization fails unless {@code WebCaptorRuntimeHints} registered the binding metadata,
 * so this test is exactly the gap-catcher. No Mockito — real MockMvc + real objects only.
 *
 * <p>The query-param assertions also cover the {@code RequestQueryParamsCaptor} path whose native-only
 * empty-{@code getParameterMap()} fallback was the original bug.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@Tag("native")
class WebCaptorNativeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventCaptureListener eventCaptureListener;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        eventCaptureListener.clearEvents();
    }

    @Test
    void capturesAndSerializesRequestEventUnderNative() throws Exception {
        mockMvc.perform(get("/test/query/basic?color=blue&size=large"))
                .andExpect(status().isOk());

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getQueryParams().get("color")).containsExactly("blue");
        assertThat(req.getQueryParams().get("size")).containsExactly("large");

        // Serialization is what breaks under native without the registered binding hints. (Consumers
        // serialize the event and bind it into a tree/map/audit row; the interface-typed queryParams field
        // is not deserialized straight back into the event type, so we assert against the parsed tree.)
        String json = objectMapper.writeValueAsString(req);
        assertThat(json).contains("color").contains("blue");

        com.fasterxml.jackson.databind.JsonNode tree = objectMapper.readTree(json);
        assertThat(tree.get("queryParams").get("color").get(0).asText()).isEqualTo("blue");
        assertThat(tree.get("path").asText()).isEqualTo(req.getPath());
    }

    @Test
    void capturesAndSerializesResponseEventUnderNative() throws Exception {
        mockMvc.perform(get("/test/query/basic?color=red"))
                .andExpect(status().isOk());

        HttpResponseEvent res = eventCaptureListener.getResponseEvents().getFirst();
        assertThat(res.getResponseStatus().value()).isEqualTo(200);

        String json = objectMapper.writeValueAsString(res);
        assertThat(json).isNotBlank();
        com.fasterxml.jackson.databind.JsonNode tree = objectMapper.readTree(json);
        assertThat(tree).isNotNull();
        assertThat(tree.has("path")).isTrue();
    }
}
