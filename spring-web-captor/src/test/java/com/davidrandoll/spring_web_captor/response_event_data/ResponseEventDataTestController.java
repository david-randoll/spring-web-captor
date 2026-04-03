package com.davidrandoll.spring_web_captor.response_event_data;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/test/response-data")
public class ResponseEventDataTestController {

    @PostMapping("/{id}")
    public ResponseEntity<Map<String, Object>> postWithEverything(
            @PathVariable String id,
            @RequestParam(required = false) Map<String, String> queryParams,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-Custom-Header", required = false) String customHeader) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", id);
        response.put("queryParams", queryParams);
        response.put("body", body);
        response.put("customHeader", customHeader);
        return ResponseEntity.ok(response);
    }
}
