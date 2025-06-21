package com.davidrandoll.spring_web_captor.content_type;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/test/content-type")
public class ContentTypeTestController {

    @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> handleJson(@RequestBody Map<String, Object> payload) {
        return Map.of("received", payload);
    }

    @PostMapping(value = "/xml", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> handleXml(@RequestBody String xml) {
        return ResponseEntity.ok("<response>" + xml + "</response>");
    }

    @PostMapping(value = "/form", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String handleForm(@RequestParam Map<String, String> formData) {
        return "Form received: " + formData;
    }

    @PostMapping(value = "/text", consumes = MediaType.TEXT_PLAIN_VALUE)
    public String handleText(@RequestBody String body) {
        return "Text received: " + body;
    }
}