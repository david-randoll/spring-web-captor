package com.davidrandoll.spring_web_captor.request_body;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/test/body")
public class RequestBodyTestController {

    @PostMapping("/json")
    public ResponseEntity<String> json(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok("JSON OK");
    }

    @PostMapping(value = "/text", consumes = "text/plain")
    public ResponseEntity<String> plainText(@RequestBody String body) {
        return ResponseEntity.ok("Text OK");
    }

    @PostMapping(value = "/xml", consumes = "application/xml")
    public ResponseEntity<String> xml(@RequestBody String body) {
        return ResponseEntity.ok("XML OK");
    }

    @PostMapping(value = "/empty")
    public ResponseEntity<String> empty(@RequestBody(required = false) String body) {
        return ResponseEntity.ok(body == null ? "No Body" : "Body Present");
    }

    @PostMapping(value = "/form", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> form(@RequestParam Map<String, String> form) {
        return ResponseEntity.ok("Form OK");
    }

    @PostMapping(value = "/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> multipart(@RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "description", required = false) String description) {
        return ResponseEntity.ok("Multipart OK");
    }
}

