package com.davidrandoll.spring_web_captor.query_params;

import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/test/query")
public class QueryParamTestController {

    @GetMapping("/basic")
    public ResponseEntity<String> basic(@RequestParam(required = false) Map<String, String> params) {
        return ResponseEntity.ok("Basic OK");
    }

    @GetMapping("/multi")
    public ResponseEntity<String> multi(@RequestParam(required = false) MultiValueMap<String, String> params) {
        return ResponseEntity.ok("Multi OK");
    }

    @GetMapping("/structured")
    public ResponseEntity<String> structured(@RequestParam(required = false) Map<String, String> params) {
        return ResponseEntity.ok("Structured OK");
    }
}