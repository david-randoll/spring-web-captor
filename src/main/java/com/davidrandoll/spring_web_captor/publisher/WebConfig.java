package com.davidrandoll.spring_web_captor.publisher;

import com.davidrandoll.spring_web_captor.properties.IsWebCaptorEnabled;
import com.davidrandoll.spring_web_captor.publisher.request.HttpRequestEventPublisher;
import com.davidrandoll.spring_web_captor.publisher.response.RuntimeExceptionResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration("webCaptorWebConfig")
@EnableWebMvc
@RequiredArgsConstructor
@Conditional(IsWebCaptorEnabled.class)
public class WebConfig implements WebMvcConfigurer {
    private final HttpRequestEventPublisher httpRequestEventPublisher;
    private final RuntimeExceptionResolver runtimeExceptionResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(httpRequestEventPublisher).addPathPatterns("/**");
    }

    @Override
    public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        resolvers.add(runtimeExceptionResolver);
    }
}
