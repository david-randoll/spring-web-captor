package com.davidrandoll.spring_web_captor.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/test/error")
public class ErrorTestController {

    @GetMapping("/runtime")
    public ResponseEntity<String> throwRuntime() {
        throw new RuntimeException("Simulated runtime exception");
    }

    @GetMapping("/nullpointer")
    public ResponseEntity<String> throwNPE() {
        String str = null;
        str.length(); // NPE
        return ResponseEntity.ok("Won't reach here");
    }

    @GetMapping("/custom")
    public ResponseEntity<String> customException() {
        throw new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT, "I'm a teapot");
    }

    @GetMapping("/notfound")
    public ResponseEntity<String> notFound() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
    }

    @PostMapping("/validation")
    public ResponseEntity<String> validationError(@RequestBody @Valid DummyRequest request) {
        return ResponseEntity.ok("Valid request");
    }

    @Getter
    @Setter
    public static class DummyRequest {
        @NotBlank
        private String name;
    }
}