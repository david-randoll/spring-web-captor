package com.davidrandoll.spring_web_captor.app_property.conditional_path;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/filter")
class FilterTestController {

    @GetMapping("/**")
    public ResponseEntity<String> get() {
        return ResponseEntity.ok("GET OK");
    }

    @PostMapping("/**")
    public ResponseEntity<String> post() {
        return ResponseEntity.ok("POST OK");
    }

    @PutMapping("/**")
    public ResponseEntity<String> put() {
        return ResponseEntity.ok("PUT OK");
    }

    @GetMapping("/none")
    public ResponseEntity<String> none() {
        return ResponseEntity.ok("NO EXCLUDE");
    }
}