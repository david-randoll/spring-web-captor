package com.davidrandoll.spring_web_captor.query_params;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.setup.EventCaptureListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpRequestEventQueryParamTest {
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
    void testNoQueryParams() throws Exception {
        mockMvc.perform(get("/test/query/basic"))
                .andExpect(status().isOk());

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getQueryParams()).isEmpty();
    }

    @Test
    void testSingleQueryParam() throws Exception {
        mockMvc.perform(get("/test/query/basic?color=blue"))
                .andExpect(status().isOk());

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getQueryParams().get("color")).containsExactly("blue");
    }

    @Test
    void testMultipleQueryParams() throws Exception {
        mockMvc.perform(get("/test/query/basic?color=red&size=large"))
                .andExpect(status().isOk());

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getQueryParams().get("color")).containsExactly("red");
        assertThat(req.getQueryParams().get("size")).containsExactly("large");
    }

    @Test
    void testDuplicateQueryParams() throws Exception {
        mockMvc.perform(get("/test/query/multi?color=blue&color=green"))
                .andExpect(status().isOk());

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getQueryParams().get("color")).containsExactly("blue", "green");
    }

    @Test
    void testEmptyValueQueryParam() throws Exception {
        mockMvc.perform(get("/test/query/basic?color="))
                .andExpect(status().isOk());

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getQueryParams().get("color")).containsExactly("");
    }

    @Test
    void testKeyOnlyQueryParam() throws Exception {
        mockMvc.perform(get("/test/query/basic?flag"))
                .andExpect(status().isOk());

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getQueryParams()).containsKey("flag");
        assertThat(req.getQueryParams().get("flag").getFirst()).isNull();
    }

    @Test
    void testEncodedCharactersInQueryParam() throws Exception {
        mockMvc.perform(get("/test/query/basic?message=50%25+off"))
                .andExpect(status().isOk());

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getQueryParams().get("message")).containsExactly("50%25+off");
    }

    @Test
    void testStructuredQueryParamKey() throws Exception {
        mockMvc.perform(get("/test/query/structured?filter[price][gte]=100"))
                .andExpect(status().isOk());

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getQueryParams().get("filter[price][gte]")).containsExactly("100");
    }

    @Test
    void testJsonLikeQueryParamValue() throws Exception {
        String json = URLEncoder.encode("{\"name\":\"david\"}", StandardCharsets.UTF_8);
        mockMvc.perform(get("/test/query/basic?data=" + json))
                .andExpect(status().isOk());

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getQueryParams().get("data")).containsExactly(json);
    }

    @Test
    void testLongQueryParamList() throws Exception {
        StringBuilder queryBuilder = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            queryBuilder.append("key").append(i).append("=val").append(i).append("&");
        }

        mockMvc.perform(get("/test/query/basic?" + queryBuilder))
                .andExpect(status().isOk());

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getQueryParams().get("key99")).containsExactly("val99");
    }

    @Test
    void testExtraAmpersands() throws Exception {
        mockMvc.perform(get("/test/query/basic?&&&color=green"))
                .andExpect(status().isOk());

        HttpRequestEvent req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getQueryParams().get("color")).containsExactly("green");
    }

    @Test
    void testBasicQueryParam() throws Exception {
        mockMvc.perform(get("/test/query/basic").queryParam("color", "red"))
                .andExpect(status().isOk());

        var req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getQueryParams().get("color")).containsExactly("red");
    }

    @Test
    void testMissingQueryParam() throws Exception {
        mockMvc.perform(get("/test/query/basic"))
                .andExpect(status().isOk());

        var req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getQueryParams().containsKey("color")).isFalse();
    }

    @Test
    void testEmptyQueryParamValue() throws Exception {
        mockMvc.perform(get("/test/query/basic").queryParam("empty", ""))
                .andExpect(status().isOk());

        var req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getQueryParams().get("empty")).containsExactly("");
    }

    @Test
    void testMultipleQueryParamValues() throws Exception {
        mockMvc.perform(get("/test/query/multi")
                        .queryParam("color", "red")
                        .queryParam("color", "green"))
                .andExpect(status().isOk());

        var req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getQueryParams().get("color")).containsExactly("red", "green");
    }

    @Test
    void testSpecialCharactersInQueryParams() throws Exception {
        mockMvc.perform(get("/test/query/basic")
                        .queryParam("q", "name@domain.com"))
                .andExpect(status().isOk());

        var req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getQueryParams().get("q")).containsExactly("name@domain.com");
    }

    @Test
    void testCaseSensitiveQueryParam() throws Exception {
        mockMvc.perform(get("/test/query/basic")
                        .queryParam("Name", "David")
                        .queryParam("name", "Randoll"))
                .andExpect(status().isOk());

        var req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getQueryParams().get("Name")).containsExactly("David");
        assertThat(req.getQueryParams().get("name")).containsExactly("Randoll");
    }

    @Test
    void testUrlEncodedQueryParam() throws Exception {
        mockMvc.perform(get("/test/query/basic")
                        .queryParam("q", "hello world!@#"))
                .andExpect(status().isOk());

        var req = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(req.getQueryParams().get("q")).containsExactly("hello world!@#");
    }

    @Test
    void testParamWithSpaces() throws Exception {
        mockMvc.perform(get("/test/query/basic?name=John Doe"))
                .andExpect(status().isOk());

        var event = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(event.getQueryParams().get("name")).containsExactly("John Doe");
    }

    @Test
    void testParamWithSpecialCharacters() throws Exception {
        mockMvc.perform(get("/test/query/basic?symbols=@$!/"))
                .andExpect(status().isOk());

        var event = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(event.getQueryParams().get("symbols")).containsExactly("@$!/");
    }

    @Test
    void testEmptyQueryString() throws Exception {
        mockMvc.perform(get("/test/query/basic?"))
                .andExpect(status().isOk());

        var event = eventCaptureListener.getRequestEvents().getFirst();
        assertTrue(event.getQueryParams().isEmpty());
    }

    @Test
    void testQueryStringWithOnlyDelimiters() throws Exception {
        mockMvc.perform(get("/test/query/basic?&&"))
                .andExpect(status().isOk());

        var event = eventCaptureListener.getRequestEvents().getFirst();
        assertTrue(event.getQueryParams().isEmpty());
    }

    @Test
    void testEqualsWithoutValue() throws Exception {
        mockMvc.perform(get("/test/query/basic?keyOnly="))
                .andExpect(status().isOk());

        var event = eventCaptureListener.getRequestEvents().getFirst();
        assertTrue(event.getQueryParams().containsKey("keyOnly"));
        assertThat(event.getQueryParams().get("keyOnly")).containsExactly("");
    }

    @Test
    void testUnencodedBracketsInKeys() throws Exception {
        mockMvc.perform(get("/test/query/basic?filter[status]=active"))
                .andExpect(status().isOk());

        var event = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(event.getQueryParams().get("filter[status]")).containsExactly("active");
    }

    @Test
    void testEncodedBracketsInKeys() throws Exception {
        mockMvc.perform(get("/test/query/basic?filter[type]=pdf"))
                .andExpect(status().isOk());

        var event = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(event.getQueryParams().get("filter[type]")).containsExactly("pdf");
    }

    @Test
    void testNullValueParamsAreNotLost() throws Exception {
        mockMvc.perform(get("/test/query/basic?foo&bar=123"))
                .andExpect(status().isOk());

        var event = eventCaptureListener.getRequestEvents().getFirst();
        assertTrue(event.getQueryParams().containsKey("foo"));
        assertNull(event.getQueryParams().get("foo").getFirst());
        assertThat(event.getQueryParams().get("bar")).containsExactly("123");
    }

    @Test
    void testParamKeyCaseSensitivity() throws Exception {
        mockMvc.perform(get("/test/query/basic?Key=value1&key=value2"))
                .andExpect(status().isOk());

        var event = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(event.getQueryParams().get("Key")).containsExactly("value1");
        assertThat(event.getQueryParams().get("key")).containsExactly("value2");
    }

    @Test
    void testMultiValueOrderingPreserved() throws Exception {
        mockMvc.perform(get("/test/query/multi?status=active&status=pending&status=done"))
                .andExpect(status().isOk());

        var event = eventCaptureListener.getRequestEvents().getFirst();
        assertThat(event.getQueryParams().get("status"))
                .containsExactly("active", "pending", "done");
    }
}
