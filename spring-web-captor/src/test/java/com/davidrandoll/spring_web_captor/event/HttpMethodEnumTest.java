package com.davidrandoll.spring_web_captor.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HttpMethodEnumTest {

    @Test
    void testFromValue_allStandardMethods() {
        for (HttpMethodEnum method : HttpMethodEnum.values()) {
            assertThat(HttpMethodEnum.fromValue(method.getMethod()))
                    .isEqualTo(method);
        }
    }

    @Test
    void testFromValue_byEnumName() {
        for (HttpMethodEnum method : HttpMethodEnum.values()) {
            assertThat(HttpMethodEnum.fromValue(method.name()))
                    .isEqualTo(method);
        }
    }

    @Test
    void testFromValue_caseInsensitive_lowercase() {
        assertThat(HttpMethodEnum.fromValue("get")).isEqualTo(HttpMethodEnum.GET);
        assertThat(HttpMethodEnum.fromValue("post")).isEqualTo(HttpMethodEnum.POST);
        assertThat(HttpMethodEnum.fromValue("delete")).isEqualTo(HttpMethodEnum.DELETE);
    }

    @Test
    void testFromValue_caseInsensitive_mixedCase() {
        assertThat(HttpMethodEnum.fromValue("gEt")).isEqualTo(HttpMethodEnum.GET);
        assertThat(HttpMethodEnum.fromValue("Post")).isEqualTo(HttpMethodEnum.POST);
        assertThat(HttpMethodEnum.fromValue("DeLeTe")).isEqualTo(HttpMethodEnum.DELETE);
    }

    @Test
    void testFromValue_null_returnsUnknown() {
        assertThat(HttpMethodEnum.fromValue(null)).isEqualTo(HttpMethodEnum.UNKNOWN);
    }

    @Test
    void testFromValue_emptyString_returnsUnknown() {
        assertThat(HttpMethodEnum.fromValue("")).isEqualTo(HttpMethodEnum.UNKNOWN);
    }

    @Test
    void testFromValue_invalidString_returnsUnknown() {
        assertThat(HttpMethodEnum.fromValue("INVALID")).isEqualTo(HttpMethodEnum.UNKNOWN);
        assertThat(HttpMethodEnum.fromValue("FETCH")).isEqualTo(HttpMethodEnum.UNKNOWN);
        assertThat(HttpMethodEnum.fromValue("REMOVE")).isEqualTo(HttpMethodEnum.UNKNOWN);
    }

    @Test
    void testFromValue_trace() {
        assertThat(HttpMethodEnum.fromValue("TRACE")).isEqualTo(HttpMethodEnum.TRACE);
    }

    @Test
    void testFromValue_connect() {
        assertThat(HttpMethodEnum.fromValue("CONNECT")).isEqualTo(HttpMethodEnum.CONNECT);
    }
}
