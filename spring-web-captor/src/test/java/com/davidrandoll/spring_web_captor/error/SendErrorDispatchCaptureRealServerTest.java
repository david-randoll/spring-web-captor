package com.davidrandoll.spring_web_captor.error;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.setup.EventCaptureListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end version of {@link SendErrorDispatchCaptureTest} that runs against a real embedded
 * Tomcat (rather than MockMvc) so the Servlet container actually performs the ERROR dispatch
 * triggered by {@code sendError}. This is what proves the captor captures the <em>final</em>
 * status (403) and the body that {@code /error}'s {@code BasicErrorController} writes —
 * the exact production scenario observed on the calculation service.
 *
 * <p>MockMvc with {@code TestDispatcherServlet} only simulates {@code sendError} at the response
 * level — it doesn't kick off the error-page dispatch that real Tomcat performs. So we use
 * {@link SpringBootTest.WebEnvironment#RANDOM_PORT} here to get a real container.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {WebCaptorApplication.class, SendErrorDispatchCaptureRealServerTest.TestConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SendErrorDispatchCaptureRealServerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EventCaptureListener eventCaptureListener;

    @BeforeEach
    void setUp() {
        eventCaptureListener.clearEvents();
    }

    @Test
    void realErrorDispatch_capturesFinal403Status() {
        ResponseEntity<String> response = restTemplate.getForEntity("/captor-real-test/throw-translated", String.class);

        // The HTTP response the client sees: 403, translated by our SimulatedExceptionTranslationFilter.
        assertThat(response.getStatusCode().value())
                .as("client must receive 403 from the outer sendError")
                .isEqualTo(403);

        // The captor must publish a response event with status 403 (the FINAL status set by
        // sendError + rendered by /error), not the stale 200 from before sendError fired upstream.
        // Use Awaitility because the captor publishes via Spring events on an async-friendly path.
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            HttpResponseEvent finalEvent = eventCaptureListener.getResponseEvents().stream()
                    .filter(e -> e.getResponseStatus() != null && e.getResponseStatus().value() == 403)
                    .reduce((first, second) -> second) // pick the latest if multiple are present
                    .orElseThrow(() -> new AssertionError(
                            "Expected a captured response event with status 403, got: " +
                                    eventCaptureListener.getResponseEvents().stream()
                                            .map(e -> e.getResponseStatus() == null ? "null" : String.valueOf(e.getResponseStatus().value()))
                                            .toList()));
            assertThat(finalEvent.getResponseStatus().value()).isEqualTo(403);
        });
    }

    @Test
    void realErrorDispatch_capturesResponseBodyWritten() {
        restTemplate.getForEntity("/captor-real-test/throw-translated", String.class);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            HttpResponseEvent finalEvent = eventCaptureListener.getResponseEvents().stream()
                    .filter(e -> e.getResponseStatus() != null && e.getResponseStatus().value() == 403)
                    .reduce((first, second) -> second)
                    .orElseThrow(() -> new AssertionError("no 403 response event"));

            // The error dispatch writes whatever body the configured /error handler renders —
            // in production with BasicErrorController this is the JSON error envelope; in this
            // test environment it may be the sendError message ("Forbidden") rendered as text/JSON.
            // The captor's job is to capture <em>whatever</em> the dispatch wrote, not nothing.
            assertThat(finalEvent.getResponseBody())
                    .as("captor must capture the body the error dispatch wrote")
                    .isNotNull();
            String bodyAsText = finalEvent.getResponseBody().toString();
            assertThat(bodyAsText)
                    .as("body must not be empty — that's the calculation-service bug we fixed")
                    .isNotEmpty();
        });
    }

    @Test
    void realErrorDispatch_neverPublishesStale200ResponseEvent() {
        restTemplate.getForEntity("/captor-real-test/throw-translated", String.class);

        // Wait long enough for any straggling async publish to land.
        await().pollDelay(Duration.ofMillis(500)).atMost(Duration.ofSeconds(2)).until(() -> true);

        // The regression: a 200 publish from the original-dispatch catch block would race with
        // — and overwrite — the 403 publish from the error dispatch. The fix removed the
        // stale 200 publish; assert none exists.
        assertThat(eventCaptureListener.getResponseEvents())
                .as("no stale 200 should be captured for an exception that was translated by sendError")
                .noneMatch(e -> e.getResponseStatus() != null && e.getResponseStatus().value() == 200);
    }

    // ----------------------- Test fixtures -----------------------

    @Configuration
    static class TestConfig {
        @Bean
        FilterRegistrationBean<SimulatedExceptionTranslationFilter> simulatedExceptionTranslationFilter() {
            FilterRegistrationBean<SimulatedExceptionTranslationFilter> reg = new FilterRegistrationBean<>(new SimulatedExceptionTranslationFilter());
            reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
            reg.addUrlPatterns("/captor-real-test/*");
            return reg;
        }

        @Bean
        CaptorRealTestController captorRealTestController() {
            return new CaptorRealTestController();
        }
    }

    @RestController
    @RequestMapping("/captor-real-test")
    static class CaptorRealTestController {
        @GetMapping("/throw-translated")
        public String throwTranslated() {
            // Use a real Spring Security AccessDeniedException so the captor's
            // UnhandledExceptionResponseFilter defers it (re-throws) rather than swallowing
            // it as a 500. This matches what @PreAuthorize denials throw in production.
            throw new AccessDeniedException("denied");
        }
    }

    /**
     * Outer servlet filter that mimics Spring Security's ExceptionTranslationFilter: catches the
     * AccessDeniedException and calls {@code response.sendError(403)} so Tomcat performs an error
     * dispatch and {@code BasicErrorController} renders the body.
     */
    static class SimulatedExceptionTranslationFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(@NonNull HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        @NonNull FilterChain chain) throws ServletException, IOException {
            try {
                chain.doFilter(request, response);
            } catch (ServletException e) {
                if (e.getCause() instanceof AccessDeniedException) {
                    response.sendError(HttpStatus.FORBIDDEN.value(), "Forbidden");
                    return;
                }
                throw e;
            } catch (AccessDeniedException ex) {
                response.sendError(HttpStatus.FORBIDDEN.value(), "Forbidden");
            }
        }
    }
}
