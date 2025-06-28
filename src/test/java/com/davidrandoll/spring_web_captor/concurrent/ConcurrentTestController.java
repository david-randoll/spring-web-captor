package com.davidrandoll.spring_web_captor.concurrent;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/concurrent")
public class ConcurrentTestController {

    @GetMapping("/echo")
    public ResponseEntity<String> echo(@RequestParam String message) throws InterruptedException {
        Thread.sleep(100); // simulate processing delay
        return ResponseEntity.ok()
                .header("X-Message", message)
                .body("Echo: " + message);
    }

    @GetMapping("/maybe-fail")
    public ResponseEntity<String> maybeFail(@RequestParam int index) {
        if (index % 3 == 0) {
            throw new RuntimeException("Intentional failure for index " + index);
        }
        return ResponseEntity.ok("Index " + index + " OK");
    }

}
