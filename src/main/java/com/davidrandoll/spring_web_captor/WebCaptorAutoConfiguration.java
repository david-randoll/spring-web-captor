package com.davidrandoll.spring_web_captor;

import com.davidrandoll.spring_web_captor.body_parser.registry.DefaultBodyParserRegistry;
import com.davidrandoll.spring_web_captor.body_parser.registry.IBodyParserRegistry;
import com.davidrandoll.spring_web_captor.extensions.HttpDurationFilterExtension;
import com.davidrandoll.spring_web_captor.extensions.IHttpEventExtension;
import com.davidrandoll.spring_web_captor.extensions.IpAddressHttpEventExtension;
import com.davidrandoll.spring_web_captor.extensions.UserAgentHttpEventExtension;
import com.davidrandoll.spring_web_captor.properties.IsDurationEnabled;
import com.davidrandoll.spring_web_captor.properties.IsIpAddressEnabled;
import com.davidrandoll.spring_web_captor.properties.IsUserAgentEnabled;
import com.davidrandoll.spring_web_captor.properties.IsWebCaptorEnabled;
import com.davidrandoll.spring_web_captor.publisher.DefaultWebCaptorEventPublisher;
import com.davidrandoll.spring_web_captor.publisher.IWebCaptorEventPublisher;
import com.davidrandoll.spring_web_captor.publisher.request.HttpRequestEventPublisher;
import com.davidrandoll.spring_web_captor.publisher.response.HttpResponseEventPublisher;
import com.davidrandoll.spring_web_captor.publisher.response.RuntimeExceptionResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;

@AutoConfiguration
@ComponentScan
@ConfigurationPropertiesScan
public class WebCaptorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @Conditional(IsWebCaptorEnabled.class)
    public IWebCaptorEventPublisher webCaptorEventPublisher(ApplicationEventPublisher publisher) {
        return new DefaultWebCaptorEventPublisher(publisher);
    }

    @Bean
    @ConditionalOnMissingBean
    @Conditional(IsWebCaptorEnabled.class)
    public IBodyParserRegistry bodyParserRegistry(ObjectMapper objectMapper) {
        return new DefaultBodyParserRegistry(objectMapper);
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
            IWebCaptorEventPublisher publisher,
            DefaultErrorAttributes defaultErrorAttributes,
            List<IHttpEventExtension> httpEventExtensions,
            IBodyParserRegistry bodyParserRegistry
    ) {
        return new HttpResponseEventPublisher(publisher, defaultErrorAttributes, httpEventExtensions, bodyParserRegistry);
    }

    @Bean("httpRequestEventPublisher")
    @ConditionalOnMissingBean(name = "httpRequestEventPublisher", ignored = HttpRequestEventPublisher.class)
    @Conditional(IsWebCaptorEnabled.class)
    public HttpRequestEventPublisher httpRequestEventPublisher(
            IWebCaptorEventPublisher publisher,
            List<IHttpEventExtension> httpEventExtensions,
            IBodyParserRegistry bodyParserRegistry
    ) {
        return new HttpRequestEventPublisher(publisher, httpEventExtensions, bodyParserRegistry);
    }

    @Bean("runtimeExceptionResolver")
    @ConditionalOnMissingBean(name = "runtimeExceptionResolver", ignored = RuntimeExceptionResolver.class)
    @Conditional(IsWebCaptorEnabled.class)
    public RuntimeExceptionResolver runtimeExceptionResolver(ObjectMapper mapper) {
        return new RuntimeExceptionResolver(mapper);
    }
}