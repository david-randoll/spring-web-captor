package com.davidrandoll.spring_web_captor.storage.aot;

import com.davidrandoll.spring_web_captor.storage.NetworkLogProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkLogStorageRuntimeHintsTest {

    private final RuntimeHints hints = new RuntimeHints();

    NetworkLogStorageRuntimeHintsTest() {
        new NetworkLogStorageRuntimeHints().registerHints(hints, getClass().getClassLoader());
    }

    @Test
    @DisplayName("registers binding hints for NetworkLogProperties and its nested CaptureRule")
    void registersConfigurationProperties() {
        assertThat(RuntimeHintsPredicates.reflection().onType(NetworkLogProperties.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(NetworkLogProperties.CaptureRule.class)).accepts(hints);
    }
}
