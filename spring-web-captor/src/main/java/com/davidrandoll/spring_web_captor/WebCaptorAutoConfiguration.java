package com.davidrandoll.spring_web_captor;

import com.davidrandoll.spring_web_captor.config.AppConfig;
import com.davidrandoll.spring_web_captor.config.WebConfig;
import com.davidrandoll.spring_web_captor.properties.WebCaptorProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({
        AppConfig.class,
        WebConfig.class,
        WebCaptorProperties.class
})
public class WebCaptorAutoConfiguration {
}