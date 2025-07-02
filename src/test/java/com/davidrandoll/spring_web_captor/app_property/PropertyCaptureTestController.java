package com.davidrandoll.spring_web_captor.app_property;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok("Uploaded: " + file.getOriginalFilename());
    }

    @GetMapping("/response-headers")
    public ResponseEntity<String> responseHeaders() {
        return ResponseEntity.ok()
                .header("X-Custom-Header", "CustomValue")
                .body("Headers OK");
    }

    @GetMapping("/response-status/{code}")
    public ResponseEntity<String> responseStatus(@PathVariable int code) {
        return ResponseEntity.status(code).body("Status " + code);
    }

    @GetMapping("/response-body")
    public ResponseEntity<String> responseBody() {
        return ResponseEntity.ok("Response Body Content");
    }

    @GetMapping("/error-detail")
    public ResponseEntity<String> errorDetail() {
        throw new RuntimeException("Simulated error for testing");
    }

}