package com.davidrandoll.spring_web_captor.app_property;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/property")
public class PropertyCaptureTestController {
    @PostMapping("/echo")
    public ResponseEntity<String> echo(@RequestBody String body) {
        return ResponseEntity.ok("OK");
    }
}
