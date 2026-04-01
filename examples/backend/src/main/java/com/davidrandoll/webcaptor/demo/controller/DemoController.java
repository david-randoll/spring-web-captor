package com.davidrandoll.webcaptor.demo.controller;

import com.davidrandoll.webcaptor.demo.model.DemoItem;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/demo")
public class DemoController {

    private final AtomicLong idCounter = new AtomicLong(3);
    private final ConcurrentHashMap<Long, DemoItem> items = new ConcurrentHashMap<>(Map.of(
            1L, DemoItem.builder().id(1L).name("Laptop").description("High-performance laptop").tags(List.of("electronics", "computing")).createdAt(LocalDateTime.now()).build(),
            2L, DemoItem.builder().id(2L).name("Headphones").description("Noise-canceling headphones").tags(List.of("electronics", "audio")).createdAt(LocalDateTime.now()).build(),
            3L, DemoItem.builder().id(3L).name("Keyboard").description("Mechanical keyboard").tags(List.of("electronics", "peripherals")).createdAt(LocalDateTime.now()).build()
    ));

    // --- JSON CRUD ---

    @GetMapping("/items")
    public List<DemoItem> listItems() {
        return List.copyOf(items.values());
    }

    @GetMapping("/items/{id}")
    public DemoItem getItem(@PathVariable Long id) {
        DemoItem item = items.get(id);
        if (item == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + id);
        return item;
    }

    @PostMapping("/items")
    public ResponseEntity<DemoItem> createItem(@Valid @RequestBody DemoItem item) {
        long id = idCounter.incrementAndGet();
        item.setId(id);
        item.setCreatedAt(LocalDateTime.now());
        items.put(id, item);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @PutMapping("/items/{id}")
    public DemoItem updateItem(@PathVariable Long id, @Valid @RequestBody DemoItem item) {
        if (!items.containsKey(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + id);
        item.setId(id);
        item.setCreatedAt(items.get(id).getCreatedAt());
        items.put(id, item);
        return item;
    }

    @PatchMapping("/items/{id}")
    public DemoItem patchItem(@PathVariable Long id, @RequestBody Map<String, Object> patch) {
        DemoItem item = items.get(id);
        if (item == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + id);
        if (patch.containsKey("name")) item.setName((String) patch.get("name"));
        if (patch.containsKey("description")) item.setDescription((String) patch.get("description"));
        return item;
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        if (!items.containsKey(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + id);
        items.remove(id);
        return ResponseEntity.noContent().build();
    }

    // --- Form URL-encoded ---

    @PostMapping(value = "/form", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Map<String, Object> submitForm(@RequestParam Map<String, String> formData) {
        return Map.of("received", formData, "message", "Form submitted successfully");
    }

    // --- Multipart file upload ---

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false, defaultValue = "") String description) {
        return Map.of(
                "filename", file.getOriginalFilename(),
                "size", file.getSize(),
                "contentType", file.getContentType(),
                "description", description,
                "message", "File uploaded successfully"
        );
    }

    // --- Plain text ---

    @PostMapping(value = "/text", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String echoText(@RequestBody String body) {
        return "Echo: " + body;
    }

    // --- Path variables ---

    @GetMapping("/paths/{category}/{id}")
    public Map<String, String> pathVariables(@PathVariable String category, @PathVariable String id) {
        return Map.of("category", category, "id", id);
    }

    @GetMapping("/paths/wildcard/{*rest}")
    public Map<String, String> wildcardPath(@PathVariable String rest) {
        return Map.of("capturedPath", rest);
    }

    // --- Query parameters ---

    @GetMapping("/search")
    public Map<String, Object> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) List<String> filters) {
        return Map.of(
                "query", q != null ? q : "",
                "page", page,
                "size", size,
                "sort", sort != null ? sort : "",
                "filters", filters != null ? filters : List.of(),
                "totalResults", 42
        );
    }

    // --- Errors ---

    @GetMapping("/errors/{code}")
    public void triggerError(@PathVariable int code) {
        switch (code) {
            case 400 -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This is a bad request");
            case 404 -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
            case 500 -> throw new RuntimeException("Internal server error occurred");
            case 418 -> throw new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT, "I'm a teapot");
            default -> throw new ResponseStatusException(HttpStatus.valueOf(code), "Error with status " + code);
        }
    }

    @PostMapping("/errors/validation")
    public DemoItem validateItem(@Valid @RequestBody DemoItem item) {
        return item;
    }

    // --- Slow endpoint ---

    @GetMapping("/slow/{seconds}")
    public Map<String, Object> slowEndpoint(@PathVariable int seconds) throws InterruptedException {
        int capped = Math.min(seconds, 10);
        long start = System.currentTimeMillis();
        Thread.sleep(capped * 1000L);
        long elapsed = System.currentTimeMillis() - start;
        return Map.of("requestedSeconds", seconds, "actualMs", elapsed, "message", "Slow response complete");
    }
}
