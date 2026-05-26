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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Reproduces the production scenario observed on the calculation service:
 *
 * <ol>
 *   <li>A controller (or method-security advice) throws a runtime exception.</li>
 *   <li>The exception escapes the dispatcher entirely — no {@code HandlerExceptionResolver}
 *       claims it.</li>
 *   <li>An <em>outer</em> servlet filter (in production: Spring Security's
 *       {@code ExceptionTranslationFilter}) catches it and calls {@code response.sendError(...)}
 *       to translate it to a real HTTP status.</li>
 *   <li>Tomcat then performs an ERROR dispatch through the filter chain so {@code /error}'s
 *       controller can write the body.</li>
 * </ol>
 *
 * <p>Before the fix, {@code HttpResponseEventPublisher}'s catch block would publish a response
 * event using the wrapper's <em>current</em> status — still 200, because {@code sendError} hadn't
 * fired yet (it fires in the outer filter, after this filter unwinds). That stale 200 publish
 * raced with — and overwrote — the subsequent correct publish from the ERROR dispatch.
 *
 * <p>This test asserts that:
 * <ul>
 *   <li>The captor never publishes a stale 200 response event when a translator-by-sendError
 *       exists above us in the chain.</li>
 *   <li>If a response event is published, its status reflects what the outer translator set
 *       via {@code sendError}, not the pre-translation 200.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = {WebCaptorApplication.class, SendErrorDispatchCaptureTest.TestConfig.class})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SendErrorDispatchCaptureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventCaptureListener eventCaptureListener;

    @BeforeEach
    void setUp() {
        eventCaptureListener.clearEvents();
    }

    @Test
    void captorDoesNotPublishStale200_whenExceptionTranslatedBySendErrorUpstream() throws Exception {
        // The outer SimulatedExceptionTranslationFilter (see TestConfig) catches MyDeniedException
        // and calls response.sendError(403) — exactly what Spring Security's ExceptionTranslationFilter
        // does for AccessDeniedException in production.
        mockMvc.perform(get("/captor-test/throw-translated"));

        List<HttpResponseEvent> responseEvents = eventCaptureListener.getResponseEvents();

        // The critical invariant: the captor must NEVER record a 200 status for a request whose
        // controller threw an exception that was translated by an outer sendError. A 200 here is
        // the production bug we're regression-testing.
        assertThat(responseEvents)
                .as("filter must not publish a stale 200 status from before the outer sendError")
                .noneMatch(e -> e.getResponseStatus() != null && e.getResponseStatus().value() == 200);
    }

    @Test
    void requestEventStillCapturedEvenWhenResponsePublishIsDeferred() throws Exception {
        // Even though the response publish is deferred to the error dispatch, the request event
        // must still be captured on the original dispatch — otherwise the network log row never
        // gets created.
        mockMvc.perform(get("/captor-test/throw-translated"));

        assertThat(eventCaptureListener.getRequestEvents())
                .as("request event must be published exactly once, on the original dispatch")
                .isNotEmpty();
    }

    @Test
    void successfulRequest_stillPublishesNormally() throws Exception {
        // Regression: the new catch-block behavior must not break the normal happy path.
        mockMvc.perform(get("/captor-test/ok"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        assertThat(eventCaptureListener.getResponseEvents())
                .as("a 200 OK is captured for non-exception paths")
                .anyMatch(e -> e.getResponseStatus() != null && e.getResponseStatus().value() == 200);
    }

    // ----------------------- Test fixtures -----------------------

    @Configuration
    static class TestConfig {
        /**
         * Stand-in for Spring Security's {@code ExceptionTranslationFilter}: catches a known
         * exception type and calls {@code sendError} to translate. Registered with an outer
         * order so the captor's {@code HttpResponseEventPublisher} runs inside this filter's
         * call to {@code chain.doFilter} — matching the production filter composition we
         * observed on the calculation service.
         */
        @Bean
        FilterRegistrationBean<SimulatedExceptionTranslationFilter> simulatedExceptionTranslationFilter() {
            FilterRegistrationBean<SimulatedExceptionTranslationFilter> reg = new FilterRegistrationBean<>(new SimulatedExceptionTranslationFilter());
            reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
            reg.addUrlPatterns("/captor-test/*");
            return reg;
        }

        @Bean
        CaptorTestController captorTestController() {
            return new CaptorTestController();
        }
    }

    @RestController
    @RequestMapping("/captor-test")
    static class CaptorTestController {
        @GetMapping("/throw-translated")
        public String throwTranslated() {
            throw new MyDeniedException("denied");
        }

        @GetMapping("/ok")
        public String ok() {
            return "ok";
        }
    }

    /**
     * Domain-marker exception that {@link SimulatedExceptionTranslationFilter} recognizes and
     * translates via {@code sendError(403)}.
     */
    static class MyDeniedException extends RuntimeException {
        MyDeniedException(String msg) {
            super(msg);
        }
    }

    static class SimulatedExceptionTranslationFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(@NonNull HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        @NonNull FilterChain chain) throws ServletException, IOException {
            try {
                chain.doFilter(request, response);
            } catch (ServletException e) {
                if (e.getCause() instanceof MyDeniedException) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                    return;
                }
                throw e;
            } catch (MyDeniedException ex) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
            }
        }
    }
}
