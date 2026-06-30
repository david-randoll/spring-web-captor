package com.davidrandoll.spring_web_captor.storage.testapp;

import com.davidrandoll.spring_web_captor.storage.NetworkLogEnricher;
import com.davidrandoll.spring_web_captor.storage.support.InMemoryNetworkLogStore;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Minimal application that activates the storage auto-configuration end-to-end: it supplies a concrete
 * {@link com.davidrandoll.spring_web_captor.storage.INetworkLogStore} (so the listener bean is created) and a
 * {@link NetworkLogEnricher}, then lets a real HTTP request flow captor -> events -> storage listener -> store.
 */
@SpringBootApplication
public class NetworkLogStorageTestApp {

    @Bean
    public InMemoryNetworkLogStore inMemoryNetworkLogStore() {
        return new InMemoryNetworkLogStore();
    }

    @Bean
    public NetworkLogEnricher tenantEnricher() {
        return (log, event) -> {
            if (log.getAdditionalData() == null) {
                log.setAdditionalData(new java.util.HashMap<>());
            }
            log.getAdditionalData().put("enrichedTenant", "acme");
        };
    }
}
