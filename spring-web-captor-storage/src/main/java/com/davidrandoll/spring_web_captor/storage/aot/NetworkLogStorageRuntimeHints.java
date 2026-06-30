package com.davidrandoll.spring_web_captor.storage.aot;

import com.davidrandoll.spring_web_captor.storage.NetworkLogProperties;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.lang.Nullable;

/**
 * Native-image reflection metadata for the storage module's own configuration-binding type graph.
 *
 * <p>The persisted {@code INetworkLog} implementation is supplied by the consumer (typically a JPA entity or
 * document) and is registered by the consumer's own AOT processing — the library cannot know that type. What
 * the library owns and must guarantee binds under native is {@link NetworkLogProperties} and its nested
 * {@code CaptureRule}; this is normally handled by Spring Boot's {@code @ConfigurationProperties} AOT support,
 * registered here as well so binding holds even outside standard auto-configuration.</p>
 */
public class NetworkLogStorageRuntimeHints implements RuntimeHintsRegistrar {

    private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        bindingRegistrar.registerReflectionHints(hints.reflection(),
                NetworkLogProperties.class,
                NetworkLogProperties.CaptureRule.class);
    }
}
