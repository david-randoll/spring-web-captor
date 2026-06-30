package com.davidrandoll.spring_web_captor.aot;

import com.davidrandoll.spring_web_captor.event.BaseHttpEvent;
import com.davidrandoll.spring_web_captor.event.BodyPayload;
import com.davidrandoll.spring_web_captor.event.HttpMethodEnum;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.event.SerializedFile;
import com.davidrandoll.spring_web_captor.properties.WebCaptorProperties;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;

/**
 * Registers GraalVM native-image reflection metadata for the captor's event/DTO graph so that consuming
 * services can serialize/deserialize captured events (e.g. into a workflow context, an audit row, or a
 * message body) under a closed-world native image WITHOUT each consumer re-registering the library's own
 * types.
 *
 * <p>Contributed unconditionally via {@code META-INF/spring/aot.factories} (rather than
 * {@code @ImportRuntimeHints} on the auto-configuration) so the hints are present even when the consumer
 * wires the captor beans manually or the auto-config is filtered out during AOT processing.</p>
 *
 * <p>{@link BindingReflectionHintsRegistrar} walks the property graph of each seed type transitively, so the
 * nested {@code MultiValueMap}/{@code Map}/{@code JsonNode} fields are covered. The concrete container
 * implementations that appear behind interface-typed fields ({@link LinkedMultiValueMap} behind
 * {@code MultiValueMap}, {@link HttpHeaders}) are registered explicitly because the graph walk only sees the
 * declared interface type.</p>
 */
public class WebCaptorRuntimeHints implements RuntimeHintsRegistrar {

    private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        bindingRegistrar.registerReflectionHints(hints.reflection(),
                // Event graph serialized by consumers (workflow context, audit, messaging, ...).
                BaseHttpEvent.class,
                HttpRequestEvent.class,
                HttpResponseEvent.class,
                BodyPayload.class,
                SerializedFile.class,
                HttpMethodEnum.class,
                // Concrete container types hidden behind interface-typed fields.
                LinkedMultiValueMap.class,
                HttpHeaders.class,
                HttpStatus.class,
                // @ConfigurationProperties binding target + nested groups (belt-and-suspenders; Spring Boot's
                // AOT config-properties processing normally covers these, but registering here guarantees
                // binding even if the consumer activates the captor outside standard auto-configuration).
                WebCaptorProperties.class,
                WebCaptorProperties.EventDetails.class,
                WebCaptorProperties.AdditionalDetails.class,
                WebCaptorProperties.ExcludedRequest.class);
    }
}
