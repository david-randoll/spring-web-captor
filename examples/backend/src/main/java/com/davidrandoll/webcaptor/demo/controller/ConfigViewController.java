package com.davidrandoll.webcaptor.demo.controller;

import com.davidrandoll.spring_web_captor.properties.WebCaptorProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigViewController {

    private final WebCaptorProperties properties;

    @GetMapping
    public WebCaptorProperties getConfig() {
        return properties;
    }
}
