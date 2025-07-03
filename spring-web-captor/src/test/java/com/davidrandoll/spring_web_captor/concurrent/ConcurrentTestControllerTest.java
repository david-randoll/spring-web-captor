package com.davidrandoll.spring_web_captor.concurrent;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.setup.EventCaptureListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConcurrentTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventCaptureListener eventCaptureListener; // Capture published events

    @BeforeEach
    void setUp() {
        eventCaptureListener.clearEvents(); // Reset events before each test
    }

    @Test
    void testConcurrentRequests() throws Exception {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        List<Future<String>> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            results.add(executor.submit(() -> {
                try {
                    String message = "msg-" + index;
                    mockMvc.perform(get("/test/concurrent/echo")
                                    .param("message", message))
                            .andExpect(status().isOk())
                            .andExpect(header().string("X-Message", message))
                            .andExpect(content().string("Echo: " + message));
                    return message;
                } finally {
                    latch.countDown();
                }
            }));
        }

        latch.await(); // wait for all threads to complete
        executor.shutdown();

        // Validate all request events were captured
        List<HttpRequestEvent> requests = eventCaptureListener.getRequestEvents();
        List<HttpResponseEvent> responses = eventCaptureListener.getResponseEvents();

        assertEquals(threadCount, requests.size(), "All request events captured");
        assertEquals(threadCount, responses.size(), "All response events captured");

        // Validate uniqueness
        Set<String> uniqueBodies = responses.stream()
                .map(r -> r.getResponseBody() != null ? r.getResponseBody().toString() : "")
                .collect(Collectors.toSet());

        assertEquals(threadCount, uniqueBodies.size(), "Each response should be unique");
    }

    @Test
    void testConcurrentRequestsWithFailures() throws Exception {
        int threadCount = 15;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        List<Future<Integer>> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            results.add(executor.submit(() -> {
                try {
                    mockMvc.perform(get("/test/concurrent/maybe-fail")
                                    .param("index", String.valueOf(index)))
                            .andExpect(index % 3 == 0 ? status().is5xxServerError() : status().isOk());
                    return index;
                } finally {
                    latch.countDown();
                }
            }));
        }

        latch.await();
        executor.shutdown();

        List<HttpRequestEvent> requestEvents = eventCaptureListener.getRequestEvents();
        List<HttpResponseEvent> responseEvents = eventCaptureListener.getResponseEvents();

        assertEquals(threadCount, requestEvents.size());
        assertEquals(threadCount, responseEvents.size());

        // Ensure error detail exists on every 3rd response
        for (HttpResponseEvent event : responseEvents) {
            int idx = Integer.parseInt(event.getQueryParams().get("index").get(0));
            if (idx % 3 == 0) {
                assertFalse(event.getErrorDetail().isEmpty(), "Expected error detail for index " + idx);
            } else {
                assertEquals("Index " + idx + " OK", event.getResponseBody().asText());
            }
        }
    }

}

