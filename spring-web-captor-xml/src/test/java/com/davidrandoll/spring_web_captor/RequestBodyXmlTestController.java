package com.davidrandoll.spring_web_captor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/body/xml")
public class RequestBodyXmlTestController {
    @PostMapping
    public ResponseEntity<String> xml(@RequestBody String body) {
        return ResponseEntity.ok("XML OK");
    }
}