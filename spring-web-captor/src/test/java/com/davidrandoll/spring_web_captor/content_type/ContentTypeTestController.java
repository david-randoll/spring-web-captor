package com.davidrandoll.spring_web_captor.content_type;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
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

    @PostMapping(value = "/params", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String handleContentTypeWithParams(@RequestBody Map<String, Object> body) {
        return "Content-Type with parameters received: " + body;
    }

    @PostMapping(value = "/malformed", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String handleMalformedContentType(@RequestBody Map<String, Object> body) {
        return "Malformed Content-Type received: " + body;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String handleUpload(@RequestParam("file") MultipartFile file,
                               @RequestParam("description") String description) throws IOException {
        return "Uploaded: " + file.getOriginalFilename() + ", Desc: " + description + ", Content: " + new String(file.getBytes());
    }

    @PostMapping("/echo")
    public ResponseEntity<Map<String, Object>> echo(@RequestBody(required = false) String body,
                                                    @RequestHeader(value = "Content-Type", required = false) String contentType) {
        Map<String, Object> response = new HashMap<>();
        response.put("receivedContentType", contentType);
        response.put("body", body);
        return ResponseEntity.ok(response);
    }
}