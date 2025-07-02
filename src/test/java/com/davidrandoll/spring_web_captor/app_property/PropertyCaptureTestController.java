package com.davidrandoll.spring_web_captor.app_property;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/property")
public class PropertyCaptureTestController {
    @PostMapping("/echo")
    public ResponseEntity<String> echo(@RequestBody String body) {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/path-param/{id}")
    public ResponseEntity<String> pathParam(@PathVariable String id) {
        return ResponseEntity.ok("ID: " + id);
    }
}
