package com.davidrandoll.spring_web_captor.event;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpResponseEventTest {

    @Test
    void testIsSuccessResponse_200() {
        HttpResponseEvent event = HttpResponseEvent.builder()
                .responseStatus(HttpStatus.OK)
                .build();
        assertThat(event.isSuccessResponse()).isTrue();
        assertThat(event.isErrorResponse()).isFalse();
        assertThat(event.isClientErrorResponse()).isFalse();
        assertThat(event.isServerErrorResponse()).isFalse();
    }

    @Test
    void testIsSuccessResponse_201() {
        HttpResponseEvent event = HttpResponseEvent.builder()
                .responseStatus(HttpStatus.CREATED)
                .build();
        assertThat(event.isSuccessResponse()).isTrue();
        assertThat(event.isErrorResponse()).isFalse();
    }

    @Test
    void testIsSuccessResponse_204() {
        HttpResponseEvent event = HttpResponseEvent.builder()
                .responseStatus(HttpStatus.NO_CONTENT)
                .build();
        assertThat(event.isSuccessResponse()).isTrue();
    }

    @Test
    void testIsClientErrorResponse_400() {
        HttpResponseEvent event = HttpResponseEvent.builder()
                .responseStatus(HttpStatus.BAD_REQUEST)
                .build();
        assertThat(event.isClientErrorResponse()).isTrue();
        assertThat(event.isErrorResponse()).isTrue();
        assertThat(event.isSuccessResponse()).isFalse();
        assertThat(event.isServerErrorResponse()).isFalse();
    }

    @Test
    void testIsClientErrorResponse_404() {
        HttpResponseEvent event = HttpResponseEvent.builder()
                .responseStatus(HttpStatus.NOT_FOUND)
                .build();
        assertThat(event.isClientErrorResponse()).isTrue();
        assertThat(event.isErrorResponse()).isTrue();
    }

    @Test
    void testIsServerErrorResponse_500() {
        HttpResponseEvent event = HttpResponseEvent.builder()
                .responseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        assertThat(event.isServerErrorResponse()).isTrue();
        assertThat(event.isErrorResponse()).isTrue();
        assertThat(event.isSuccessResponse()).isFalse();
        assertThat(event.isClientErrorResponse()).isFalse();
    }

    @Test
    void testIsServerErrorResponse_503() {
        HttpResponseEvent event = HttpResponseEvent.builder()
                .responseStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
        assertThat(event.isServerErrorResponse()).isTrue();
        assertThat(event.isErrorResponse()).isTrue();
    }

    @Test
    void testStatusHelpers_nullStatus() {
        HttpResponseEvent event = HttpResponseEvent.builder().build();
        assertThat(event.isSuccessResponse()).isFalse();
        assertThat(event.isClientErrorResponse()).isFalse();
        assertThat(event.isServerErrorResponse()).isFalse();
        assertThat(event.isErrorResponse()).isFalse();
    }

    @Test
    void testStatusHelpers_3xx_notErrorOrSuccess() {
        HttpResponseEvent event = HttpResponseEvent.builder()
                .responseStatus(HttpStatus.MOVED_PERMANENTLY)
                .build();
        assertThat(event.isSuccessResponse()).isFalse();
        assertThat(event.isClientErrorResponse()).isFalse();
        assertThat(event.isServerErrorResponse()).isFalse();
        assertThat(event.isErrorResponse()).isFalse();
    }

    @Test
    void testAddErrorDetail_setsResponseBody() {
        HttpResponseEvent event = HttpResponseEvent.builder().build();
        event.addErrorDetail(Map.of("message", "boom"));

        assertThat(event.getResponseBody()).isNotNull();
        assertThat(event.getResponseBody().asText()).isEqualTo("boom");
        assertThat(event.getErrorDetail()).containsEntry("message", "boom");
    }

    @Test
    void testAddErrorDetail_emptyMessage() {
        HttpResponseEvent event = HttpResponseEvent.builder().build();
        event.addErrorDetail(Map.of("status", 500));

        // No "message" key -> defaults to ""
        assertThat(event.getResponseBody()).isNotNull();
        assertThat(event.getResponseBody().asText()).isEqualTo("");
    }

    @Test
    void testBuilderAddErrorDetail_setsResponseBody() {
        HttpResponseEvent event = HttpResponseEvent.builder()
                .addErrorDetail(Map.of("message", "builder error"))
                .build();

        assertThat(event.getResponseBody()).isNotNull();
        assertThat(event.getResponseBody().asText()).isEqualTo("builder error");
        assertThat(event.getErrorDetail()).containsEntry("message", "builder error");
    }
}
