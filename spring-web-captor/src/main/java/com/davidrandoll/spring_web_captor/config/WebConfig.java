package com.davidrandoll.spring_web_captor.config;

import com.davidrandoll.spring_web_captor.properties.IsWebCaptorEnabled;
import com.davidrandoll.spring_web_captor.publisher.request.HttpRequestEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(value = "webCaptorWebConfig", proxyBeanMethods = false)
@EnableWebMvc
@RequiredArgsConstructor
@Conditional(IsWebCaptorEnabled.class)
public class WebConfig implements WebMvcConfigurer {
    private final HttpRequestEventPublisher httpRequestEventPublisher;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(httpRequestEventPublisher).addPathPatterns("/**");
    }
}
