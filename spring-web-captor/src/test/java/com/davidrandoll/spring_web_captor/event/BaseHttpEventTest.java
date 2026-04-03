package com.davidrandoll.spring_web_captor.event;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BaseHttpEventTest {

    @Test
    void testAddAndGetAdditionalData_singleEntry() {
        HttpRequestEvent event = HttpRequestEvent.builder().build();
        event.addAdditionalData("key", "value");

        assertThat(event.getAdditionalData("key", String.class)).isEqualTo("value");
    }

    @Test
    void testAddAdditionalData_map() {
        HttpRequestEvent event = HttpRequestEvent.builder().build();
        event.addAdditionalData(Map.of("a", 1, "b", "two"));

        assertThat(event.getAdditionalData("a", Integer.class)).isEqualTo(1);
        assertThat(event.getAdditionalData("b", String.class)).isEqualTo("two");
    }

    @Test
    void testGetAdditionalData_missingKey_returnsNull() {
        HttpRequestEvent event = HttpRequestEvent.builder().build();

        assertThat(event.getAdditionalData("nonexistent", String.class)).isNull();
    }

    @Test
    void testHasAdditionalData_presentKey() {
        HttpRequestEvent event = HttpRequestEvent.builder().build();
        event.addAdditionalData("present", "yes");

        assertThat(event.hasAdditionalData("present")).isTrue();
    }

    @Test
    void testHasAdditionalData_missingKey() {
        HttpRequestEvent event = HttpRequestEvent.builder().build();

        assertThat(event.hasAdditionalData("missing")).isFalse();
    }

    @Test
    void testRemoveAdditionalData() {
        HttpRequestEvent event = HttpRequestEvent.builder().build();
        event.addAdditionalData("toRemove", "value");
        assertThat(event.hasAdditionalData("toRemove")).isTrue();

        event.removeAdditionalData("toRemove");
        assertThat(event.hasAdditionalData("toRemove")).isFalse();
        assertThat(event.getAdditionalData("toRemove", String.class)).isNull();
    }

    @Test
    void testGetRequestBody_nullPayload_returnsNullNode() {
        HttpRequestEvent event = HttpRequestEvent.builder().build();

        assertThat(event.getRequestBody()).isNotNull();
        assertThat(event.getRequestBody().isNull()).isTrue();
    }

    @Test
    void testGetRequestFiles_nullPayload_returnsEmptyMap() {
        HttpRequestEvent event = HttpRequestEvent.builder().build();

        assertThat(event.getRequestFiles()).isNotNull();
        assertThat(event.getRequestFiles()).isEmpty();
    }

    @Test
    void testBuilderAdditionalData() {
        HttpRequestEvent event = HttpRequestEvent.builder()
                .additionalData("builderKey", "builderValue")
                .build();

        assertThat(event.getAdditionalData("builderKey", String.class)).isEqualTo("builderValue");
    }

    @Test
    void testAdditionalData_overwrite() {
        HttpRequestEvent event = HttpRequestEvent.builder().build();
        event.addAdditionalData("key", "original");
        event.addAdditionalData("key", "overwritten");

        assertThat(event.getAdditionalData("key", String.class)).isEqualTo("overwritten");
    }
}
