package com.davidrandoll.spring_web_captor;

import com.davidrandoll.spring_web_captor.publisher.IWebCaptorEventPublisher;
import com.davidrandoll.spring_web_captor.publisher.DefaultWebCaptorEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan
@ConfigurationPropertiesScan
public class WebCaptorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IWebCaptorEventPublisher webCaptorEventPublisher(ApplicationEventPublisher publisher) {
        return new DefaultWebCaptorEventPublisher(publisher);
    }
}