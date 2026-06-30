package com.davidrandoll.spring_web_captor.aot;

import com.davidrandoll.spring_web_captor.event.BaseHttpEvent;
import com.davidrandoll.spring_web_captor.event.BodyPayload;
import com.davidrandoll.spring_web_captor.event.HttpMethodEnum;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.event.SerializedFile;
import com.davidrandoll.spring_web_captor.properties.WebCaptorProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts the library self-registers its event/DTO graph for native-image binding so that consuming services
 * do not have to re-register the captor's own types.
 */
class WebCaptorRuntimeHintsTest {

    private final RuntimeHints hints = new RuntimeHints();

    WebCaptorRuntimeHintsTest() {
        new WebCaptorRuntimeHints().registerHints(hints, getClass().getClassLoader());
    }

    @Test
    @DisplayName("registers reflection/binding hints for every captured event + DTO type")
    void registersEventGraph() {
        assertThat(RuntimeHintsPredicates.reflection().onType(BaseHttpEvent.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(HttpRequestEvent.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(HttpResponseEvent.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(BodyPayload.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(SerializedFile.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(HttpMethodEnum.class)).accepts(hints);
    }

    @Test
    @DisplayName("registers the concrete container types behind interface-typed fields")
    void registersContainerTypes() {
        assertThat(RuntimeHintsPredicates.reflection().onType(LinkedMultiValueMap.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(HttpHeaders.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(HttpStatus.class)).accepts(hints);
    }

    @Test
    @DisplayName("registers @ConfigurationProperties binding targets")
    void registersConfigurationProperties() {
        assertThat(RuntimeHintsPredicates.reflection().onType(WebCaptorProperties.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(WebCaptorProperties.EventDetails.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(WebCaptorProperties.AdditionalDetails.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(WebCaptorProperties.ExcludedRequest.class)).accepts(hints);
    }
}
