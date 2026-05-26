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
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end verification that the {@code web-captor.defer-outer-filter-packages} property is
 * the documented extension point for the "outer filter translates an exception via sendError"
 * pattern. Production fix in the calculation service was Spring Security; this test proves the
 * same machinery works for a completely unrelated framework just by adding its package to the
 * configurable list — no library code change.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {WebCaptorApplication.class, ConfigurableDeferPackagesIntegrationTest.TestConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        // Extend the default Spring Security defer list with our custom framework's package.
        "web-captor.defer-outer-filter-packages[0]=org.springframework.security.",
        "web-captor.defer-outer-filter-packages[1]=com.acme.security."
})
class ConfigurableDeferPackagesIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EventCaptureListener eventCaptureListener;

    @BeforeEach
    void setUp() {
        eventCaptureListener.clearEvents();
    }

    @Test
    void customDomainExceptionFromConfiguredPackage_isTranslatedByOuterFilter_andCapturedAs418() {
        ResponseEntity<String> response = restTemplate.getForEntity("/acme-test/teapot-via-outer-filter", String.class);

        // The outer SimulatedAcmeTranslationFilter saw the exception (because the captor
        // deferred to it instead of rendering 500) and translated to 418 via sendError.
        assertThat(response.getStatusCode().value())
                .as("custom-package exception must reach the outer translator")
                .isEqualTo(418);

        // The captor's error-dispatch publish then captured the final 418, not 500.
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            HttpResponseEvent finalEvent = eventCaptureListener.getResponseEvents().stream()
                    .filter(e -> e.getResponseStatus() != null && e.getResponseStatus().value() == 418)
                    .reduce((first, second) -> second)
                    .orElseThrow(() -> new AssertionError("expected a 418 response event from the configured defer flow"));
            assertThat(finalEvent.getResponseStatus().value()).isEqualTo(418);
        });

        assertThat(eventCaptureListener.getResponseEvents())
                .as("no stale 500 should be published for a properly-translated configurable exception")
                .noneMatch(e -> e.getResponseStatus() != null && e.getResponseStatus().value() == 500);
    }

    @Configuration
    static class TestConfig {
        @Bean
        FilterRegistrationBean<SimulatedAcmeTranslationFilter> acmeFilter() {
            FilterRegistrationBean<SimulatedAcmeTranslationFilter> reg =
                    new FilterRegistrationBean<>(new SimulatedAcmeTranslationFilter());
            reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
            reg.addUrlPatterns("/acme-test/*");
            return reg;
        }

        @Bean
        AcmeTestController acmeTestController() {
            return new AcmeTestController();
        }
    }

    @RestController
    @RequestMapping("/acme-test")
    static class AcmeTestController {
        @GetMapping("/teapot-via-outer-filter")
        public String throwTeapot() {
            throw new com.acme.security.CustomDenied("teapot please");
        }
    }

    /**
     * Stand-in for an arbitrary framework's outer-filter exception translator that uses
     * {@code sendError} (the same pattern Spring Security uses). Catches the custom domain
     * exception and translates to 418 — proving that any package can plug into the same
     * machinery via configuration alone.
     */
    static class SimulatedAcmeTranslationFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(@NonNull HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        @NonNull FilterChain chain) throws ServletException, IOException {
            try {
                chain.doFilter(request, response);
            } catch (ServletException e) {
                if (e.getCause() instanceof com.acme.security.CustomDenied) {
                    response.sendError(HttpStatus.I_AM_A_TEAPOT.value(), "teapot");
                    return;
                }
                throw e;
            } catch (com.acme.security.CustomDenied ex) {
                response.sendError(HttpStatus.I_AM_A_TEAPOT.value(), "teapot");
            }
        }
    }
}
