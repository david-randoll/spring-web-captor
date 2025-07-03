package com.davidrandoll.spring_web_captor;

import com.davidrandoll.spring_web_captor.body_parser.registry.IBodyParserRegistry;
import com.davidrandoll.spring_web_captor.properties.IsWebCaptorEnabled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

@Slf4j
@Configuration
@Conditional(IsWebCaptorEnabled.class)
@RequiredArgsConstructor
public class WebCaptorXmlConfig {
    private final MappingJackson2XmlHttpMessageConverter converter;
    private final IBodyParserRegistry registry;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Registering XML body parsers for WebCaptor");
        var xmlMapper = converter.getObjectMapper();
        registry.register(new XmlRequestBodyParser(xmlMapper));
        registry.register(new XmlResponseBodyParser(xmlMapper));
    }
}