package com.davidrandoll.spring_web_captor.runtime_exception_resolver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/resolver")
public class RuntimeExceptionResolverTestController {

    @GetMapping("/runtime-error")
    public String throwRuntimeException() {
        throw new RuntimeException("Test runtime error");
    }
}
