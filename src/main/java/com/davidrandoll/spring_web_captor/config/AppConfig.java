package com.davidrandoll.spring_web_captor.config;

import com.davidrandoll.spring_web_captor.body_parser.registry.DefaultBodyParserRegistry;
import com.davidrandoll.spring_web_captor.body_parser.registry.IBodyParserRegistry;
import com.davidrandoll.spring_web_captor.extensions.HttpDurationFilterExtension;
import com.davidrandoll.spring_web_captor.extensions.IHttpEventExtension;
import com.davidrandoll.spring_web_captor.extensions.IpAddressHttpEventExtension;
import com.davidrandoll.spring_web_captor.extensions.UserAgentHttpEventExtension;
import com.davidrandoll.spring_web_captor.field_captor.registry.DefaultFieldCaptorRegistry;
import com.davidrandoll.spring_web_captor.field_captor.registry.IFieldCaptorRegistry;
import com.davidrandoll.spring_web_captor.properties.*;
import com.davidrandoll.spring_web_captor.publisher.DefaultHttpEventPublisher;
import com.davidrandoll.spring_web_captor.publisher.DefaultWebCaptorEventPublisher;
import com.davidrandoll.spring_web_captor.publisher.IHttpEventPublisher;
import com.davidrandoll.spring_web_captor.publisher.IWebCaptorEventPublisher;
import com.davidrandoll.spring_web_captor.publisher.request.HttpRequestEventPublisher;
import com.davidrandoll.spring_web_captor.publisher.response.HttpResponseEventPublisher;
import com.davidrandoll.spring_web_captor.publisher.response.RuntimeExceptionResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

@Configuration
public class AppConfig {
    @Bean
    @ConditionalOnMissingBean
    @Conditional(IsWebCaptorEnabled.class)
    public IWebCaptorEventPublisher webCaptorEventPublisher(ApplicationEventPublisher publisher) {
        return new DefaultWebCaptorEventPublisher(publisher);
    }

    @Bean
    @ConditionalOnMissingBean
    @Conditional(IsWebCaptorEnabled.class)
    public IBodyParserRegistry bodyParserRegistry(ObjectMapper objectMapper, WebCaptorProperties properties) {
        return new DefaultBodyParserRegistry(objectMapper, properties.getEventDetails());
    }

    @Bean
    @ConditionalOnMissingBean
    @Conditional(IsWebCaptorEnabled.class)
    public IHttpEventPublisher httpEventPublisher(
            IWebCaptorEventPublisher publisher,
            List<IHttpEventExtension> extensions,
            IFieldCaptorRegistry fieldCaptorRegistry
    ) {
        return new DefaultHttpEventPublisher(publisher, extensions, fieldCaptorRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    @Conditional(IsWebCaptorEnabled.class)
    public IFieldCaptorRegistry fieldCaptorRegistry(
            IBodyParserRegistry bodyParserRegistry, WebCaptorProperties properties,
            @Lazy @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver, List<IHttpEventExtension> extensions,
            IWebCaptorEventPublisher publisher, DefaultErrorAttributes errorAttributes
    ) {
        return new DefaultFieldCaptorRegistry(
                bodyParserRegistry, properties.getEventDetails(),
                resolver, extensions, publisher, errorAttributes
        );
    }

    @Bean("httpDurationFilterExtension")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnMissingBean(name = "httpDurationFilterExtension", ignored = HttpDurationFilterExtension.class)
    @Conditional({IsWebCaptorEnabled.class, IsDurationEnabled.class})
    public HttpDurationFilterExtension httpDurationFilterExtension() {
        return new HttpDurationFilterExtension();
    }

    @Bean("ipAddressHttpEventExtension")
    @ConditionalOnMissingBean(name = "ipAddressHttpEventExtension", ignored = {IpAddressHttpEventExtension.class})
    @Conditional({IsWebCaptorEnabled.class, IsIpAddressEnabled.class})
    public IpAddressHttpEventExtension ipAddressHttpEventExtension() {
        return new IpAddressHttpEventExtension();
    }

    @Bean("userAgentHttpEventExtension")
    @ConditionalOnMissingBean(name = "userAgentHttpEventExtension", ignored = {UserAgentHttpEventExtension.class})
    @Conditional({IsWebCaptorEnabled.class, IsUserAgentEnabled.class})
    public UserAgentHttpEventExtension userAgentHttpEventExtension() {
        return new UserAgentHttpEventExtension();
    }

    @Bean("httpResponseEventPublisher")
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    @ConditionalOnMissingBean(name = "httpResponseEventPublisher", ignored = HttpResponseEventPublisher.class)
    @Conditional(IsWebCaptorEnabled.class)
    public HttpResponseEventPublisher httpResponseEventPublisher(
            IHttpEventPublisher publisher,
            IBodyParserRegistry bodyParserRegistry,
            IFieldCaptorRegistry fieldCaptorRegistry
    ) {
        return new HttpResponseEventPublisher(
                publisher, bodyParserRegistry, fieldCaptorRegistry
        );
    }

    @Bean("httpRequestEventPublisher")
    @ConditionalOnMissingBean(name = "httpRequestEventPublisher", ignored = HttpRequestEventPublisher.class)
    @Conditional(IsWebCaptorEnabled.class)
    public HttpRequestEventPublisher httpRequestEventPublisher(
            IHttpEventPublisher publisher
    ) {
        return new HttpRequestEventPublisher(
                publisher
        );
    }

    @Bean("runtimeExceptionResolver")
    @ConditionalOnMissingBean(name = "runtimeExceptionResolver", ignored = RuntimeExceptionResolver.class)
    @Conditional(IsWebCaptorEnabled.class)
    public RuntimeExceptionResolver runtimeExceptionResolver(ObjectMapper mapper) {
        return new RuntimeExceptionResolver(mapper);
    }
}