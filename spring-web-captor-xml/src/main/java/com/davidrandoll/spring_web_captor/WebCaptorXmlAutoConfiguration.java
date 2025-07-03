package com.davidrandoll.spring_web_captor;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({
        WebCaptorXmlConfig.class
})
public class WebCaptorXmlAutoConfiguration {
}