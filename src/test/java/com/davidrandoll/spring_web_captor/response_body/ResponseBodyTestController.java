package com.davidrandoll.spring_web_captor.response_body;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/test/response")
public class ResponseBodyTestController {

    @GetMapping("/json")
    public ResponseEntity<Map<String, Object>> json() {
        return ResponseEntity.ok(Map.of("message", "Hello", "status", 200));
    }

    @GetMapping(value = "/text", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> text() {
        return ResponseEntity.ok("Plain text response");
    }

    @GetMapping(value = "/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> xml() {
        return ResponseEntity.ok("<response><message>Hello</message></response>");
    }

    @GetMapping("/empty")
    public ResponseEntity<Void> empty() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/null")
    public ResponseEntity<String> nullBody() {
        return ResponseEntity.ok(null);
    }

    @GetMapping("/utf8")
    public ResponseEntity<String> utf8() {
        return ResponseEntity.ok("Emoji 😃 and Unicode ✓");
    }

    @GetMapping("/special-chars")
    public ResponseEntity<String> specialChars() {
        return ResponseEntity.ok("Quotes: \" and Backslashes: \\");
    }
}

