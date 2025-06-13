package com.davidrandoll.spring_web_captor.publisher;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/http-methods")
public class HttpMethodsTestController {

    @GetMapping
    public String get() {
        return "GET response";
    }

    @PostMapping
    public String post(@RequestBody(required = false) String body) {
        return "POST response: " + body;
    }

    @PutMapping
    public String put(@RequestBody(required = false) String body) {
        return "PUT response: " + body;
    }

    @PatchMapping
    public String patch(@RequestBody(required = false) String body) {
        return "PATCH response: " + body;
    }

    @DeleteMapping
    public ResponseEntity<Void> delete() {
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> options() {
        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.HEAD)
    public ResponseEntity<Void> head() {
        return ResponseEntity.ok().build();
    }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE, value = "/with-content-type")
    public String postWithContentType(@RequestBody(required = false) String body) {
        return "POST response: " + body;
    }
}
