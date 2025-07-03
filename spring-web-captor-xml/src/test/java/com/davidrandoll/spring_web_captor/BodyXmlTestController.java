package com.davidrandoll.spring_web_captor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/body/xml")
public class BodyXmlTestController {
    @PostMapping
    public ResponseEntity<String> xml(@RequestBody String body) {
        return ResponseEntity.ok("XML OK");
    }

    @GetMapping(value = "/response", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> xml() {
        return ResponseEntity.ok("<response><message>Hello</message></response>");
    }
}