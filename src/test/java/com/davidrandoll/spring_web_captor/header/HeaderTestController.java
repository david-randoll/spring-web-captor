package com.davidrandoll.spring_web_captor.header;

import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/headers")
public class HeaderTestController {

    @GetMapping("/basic")
    public ResponseEntity<String> basic(@RequestHeader(value = "X-Test-Header", required = false) String header) {
        return ResponseEntity.ok("Basic OK: " + (header == null ? "none" : header));
    }

    @GetMapping("/multi")
    public ResponseEntity<String> multi(@RequestHeader MultiValueMap<String, String> headers) {
        return ResponseEntity.ok("Multi OK: " + headers.get("X-Multi-Header"));
    }

    @GetMapping("/required")
    public ResponseEntity<String> required(@RequestHeader("X-Required-Header") String header) {
        return ResponseEntity.ok("Required OK: " + header);
    }

    @GetMapping("/case")
    public ResponseEntity<String> caseInsensitive(@RequestHeader(value = "X-CaSe-HeAdEr", required = false) String header) {
        return ResponseEntity.ok("Case OK: " + header);
    }
}
