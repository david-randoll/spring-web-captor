package com.davidrandoll.spring_web_captor.storage.autoconfig;

import com.davidrandoll.spring_web_captor.storage.INetworkLogStore;
import com.davidrandoll.spring_web_captor.storage.NetworkLogEnricher;
import com.davidrandoll.spring_web_captor.storage.NetworkLogEventListener;
import com.davidrandoll.spring_web_captor.storage.NetworkLogHttpEventExtension;
import com.davidrandoll.spring_web_captor.storage.NetworkLogProperties;
import com.davidrandoll.spring_web_captor.storage.RequestIdProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "com.davidrandoll.spring_web_captor.event.HttpRequestEvent")
@ConditionalOnProperty(prefix = "network-log", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(NetworkLogProperties.class)
@EnableAsync
public class NetworkLogStorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RequestIdProvider requestIdProvider() {
        return RequestIdProvider.uuid();
    }

    @Bean
    @ConditionalOnMissingBean
    public NetworkLogHttpEventExtension networkLogHttpEventExtension(RequestIdProvider requestIdProvider) {
        return new NetworkLogHttpEventExtension(requestIdProvider);
    }

    @Bean
    @ConditionalOnBean(INetworkLogStore.class)
    @ConditionalOnMissingBean
    public NetworkLogEventListener networkLogEventListener(INetworkLogStore store,
                                                           NetworkLogProperties properties,
                                                           ObjectMapper objectMapper,
                                                           List<NetworkLogEnricher> enrichers) {
        return new NetworkLogEventListener(store, properties, objectMapper, enrichers);
    }
}
