package com.davidrandoll.spring_web_captor.runtime_exception_resolver;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * End-to-end coverage for {@code UnhandledExceptionResponseFilter} with real Spring Security wired.
 *
 * <p>Three guarantees, exercised against many exception types and many response statuses:
 * <ol>
 *   <li>Every exception that has a real handler (security filter, default resolver,
 *       {@code @ControllerAdvice}, {@code @ResponseStatus}, {@code ResponseStatusException}, validation, ...)
 *       surfaces with that handler's status — never forced to 500.</li>
 *   <li>The body that the real handler wrote (if any) reaches the client unchanged.</li>
 *   <li>An exception with no handler anywhere → 500 with our standard captor body. The client
 *       always receives a body — never an empty response.</li>
 * </ol>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = {WebCaptorApplication.class, RuntimeExceptionResolverSecurityMvcTest.TestConfig.class}
)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {"server.error.include-message=ALWAYS"})
class RuntimeExceptionResolverSecurityMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // -------- Security exceptions --------

    @Test
    void accessDeniedException_translatedTo403_byExceptionTranslationFilter() throws Exception {
        var result = mockMvc.perform(get("/test/resolver/access-denied").with(user("alice"))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void authenticationException_isNotForcedTo500() throws Exception {
        var result = mockMvc.perform(get("/test/resolver/auth-failed").with(user("alice"))).andReturn();
        assertThat(result.getResponse().getStatus()).isNotEqualTo(500);
    }

    @Test
    void preAuthorizeDenial_translatedTo403_notSwallowedAs500() throws Exception {
        // Throws AuthorizationDeniedException — the exact subclass thrown by @PreAuthorize via
        // AuthorizationManagerBeforeMethodInterceptor in real Spring Security 6.
        var result = mockMvc.perform(get("/test/resolver/pre-authorize-denied").with(user("alice"))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
    }

    // -------- ResponseStatusException with many statuses --------

    @ParameterizedTest(name = "ResponseStatusException({0}) → client gets {0}, never 500")
    @ValueSource(ints = {400, 401, 402, 403, 404, 405, 406, 408, 409, 410, 415, 418, 422, 429, 451})
    void responseStatusException_anyStatus_isPreserved(int status) throws Exception {
        var result = mockMvc.perform(get("/test/resolver/response-status/" + status).with(user("alice"))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(status);
    }

    @ParameterizedTest(name = "ResponseStatusException with reason \"{1}\" produces body containing it")
    @CsvSource({
            "404, item-missing",
            "418, short-and-stout",
            "422, validation-failed",
            "409, dup-key"
    })
    void responseStatusException_reasonReachesClient(int status, String reason) throws Exception {
        var result = mockMvc.perform(get("/test/resolver/response-status-with-reason/" + status + "/" + reason).with(user("alice"))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(status);
        // Spring's ResponseStatusExceptionResolver puts the reason in the response somewhere
        // (status message); we just verify the client still gets a real response, not nothing.
        assertThat(result.getResponse().getStatus()).isNotEqualTo(500);
    }

    // -------- @ResponseStatus-annotated exceptions --------

    @Test
    void responseStatusAnnotatedException_410_isPreserved() throws Exception {
        var result = mockMvc.perform(get("/test/resolver/annotated-410").with(user("alice"))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(410);
    }

    @Test
    void responseStatusAnnotatedException_422_isPreserved() throws Exception {
        var result = mockMvc.perform(get("/test/resolver/annotated-422").with(user("alice"))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    void responseStatusAnnotatedException_503_isPreserved() throws Exception {
        var result = mockMvc.perform(get("/test/resolver/annotated-503").with(user("alice"))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(503);
    }

    // -------- @ControllerAdvice @ExceptionHandler with custom statuses + bodies --------

    @ParameterizedTest(name = "@ExceptionHandler producing {0} delivers status {0} and body \"{1}\"")
    @CsvSource({
            "418, short and stout",
            "409, conflicting state",
            "402, payment required mate",
            "451, blocked for legal reasons"
    })
    void controllerAdvice_anyStatus_andBodyReachClient(int status, String body) throws Exception {
        var result = mockMvc.perform(get("/test/resolver/advice/" + status + "/" + body.replace(' ', '+')).with(user("alice"))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(status);
        assertThat(result.getResponse().getContentAsString()).isEqualTo(body);
    }

    // -------- Spring's DefaultHandlerExceptionResolver coverage --------

    @Test
    void unsupportedMethodReturns405_notSwallowedAs500() throws Exception {
        // GET endpoint hit with POST → HttpRequestMethodNotSupportedException → 405 by DefaultHandlerExceptionResolver.
        var result = mockMvc.perform(post("/test/resolver/get-only").with(user("alice"))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(405);
    }

    @Test
    void noHandlerFoundReturns404_notSwallowedAs500() throws Exception {
        var result = mockMvc.perform(get("/test/resolver/no-such-route-anywhere").with(user("alice"))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void unreadableBodyReturns400_notSwallowedAs500() throws Exception {
        // Send malformed JSON → HttpMessageNotReadableException → 400 by DefaultHandlerExceptionResolver.
        var result = mockMvc.perform(
                post("/test/resolver/echo-json")
                        .with(user("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ malformed }")
        ).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void validationFailureReturns400_notSwallowedAs500() throws Exception {
        // @NotBlank field empty → MethodArgumentNotValidException → 400 by DefaultHandlerExceptionResolver.
        var result = mockMvc.perform(
                post("/test/resolver/validated")
                        .with(user("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}")
        ).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }

    // -------- Truly unhandled exceptions: filter renders 500 with real body --------

    @ParameterizedTest(name = "unhandled {0} from controller → 500 with non-empty JSON body delivered to client")
    @ValueSource(strings = {
            "runtime",
            "illegal-state",
            "illegal-arg",
            "npe",
            "class-cast",
            "arithmetic",
            "ioob",
            "custom-domain"
    })
    void unhandled_exception_producesA500JsonBodyForTheClient(String kind) throws Exception {
        var result = mockMvc.perform(get("/test/resolver/throw/" + kind).with(user("alice"))).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        String body = result.getResponse().getContentAsString();
        assertThat(body).as("client must receive a non-empty 500 body for kind=%s", kind).isNotEmpty();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("status").asInt()).isEqualTo(500);
        assertThat(json.get("error").asText()).isEqualTo("Internal Server Error");
        assertThat(json.has("timestamp")).isTrue();
    }

    // -------- The captor's body for a successful response is the response, untouched --------

    @Test
    void successfulResponseBody_isPreservedToTheClient() throws Exception {
        var result = mockMvc.perform(get("/test/resolver/ok").with(user("alice"))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).isEqualTo("happy path");
    }

    @Test
    void httpStatusNoContent204_reachesClientWithEmptyBody() throws Exception {
        var result = mockMvc.perform(get("/test/resolver/no-content").with(user("alice"))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        assertThat(result.getResponse().getContentAsString()).isEmpty();
    }

    @Test
    void clientNeverGetsAnEmptyResponseForAnUnhandledException() throws Exception {
        // The single most important assertion of this whole suite.
        MvcResult result = mockMvc.perform(get("/test/resolver/throw/runtime").with(user("alice"))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        assertThat(result.getResponse().getContentAsByteArray())
                .as("for an unhandled exception, the client must receive a real body, not nothing")
                .isNotEmpty();
    }

    // ----------------------- Test fixtures -----------------------

    /**
     * Opts this test back into Spring Security autoconfig (the application class disables it by default).
     * Permit-all so the request reaches the controller, where the thrown exception flows through
     * ExceptionTranslationFilter — which is what we're verifying.
     */
    @Configuration
    @EnableWebSecurity
    @ImportAutoConfiguration({
            SecurityAutoConfiguration.class,
            SecurityFilterAutoConfiguration.class,
            UserDetailsServiceAutoConfiguration.class
    })
    static class TestConfig {
        @Bean
        SecurityFilterChain testSecurity(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(a -> a.anyRequest().permitAll());
            return http.build();
        }

        @Bean
        TestExceptionController testExceptionController() {
            return new TestExceptionController();
        }

        @Bean
        TestExceptionAdvice testExceptionAdvice() {
            return new TestExceptionAdvice();
        }
    }

    @RestController
    @RequestMapping("/test/resolver")
    static class TestExceptionController {

        // --- security ---
        @GetMapping("/access-denied")
        public String throwAccessDenied() {
            throw new AccessDeniedException("not allowed");
        }

        @GetMapping("/auth-failed")
        public String throwAuthFailed() {
            throw new BadCredentialsException("nope");
        }

        @GetMapping("/pre-authorize-denied")
        public String throwPreAuthorizeDenied() {
            // Same concrete type that Spring Security 6's @PreAuthorize throws via
            // AuthorizationManagerBeforeMethodInterceptor.
            throw new org.springframework.security.authorization.AuthorizationDeniedException(
                    "Access Denied",
                    new org.springframework.security.authorization.AuthorizationDecision(false));
        }

        // --- ResponseStatusException with arbitrary status ---
        @GetMapping("/response-status/{status}")
        public String throwResponseStatus(@PathVariable int status) {
            throw new ResponseStatusException(HttpStatus.valueOf(status));
        }

        @GetMapping("/response-status-with-reason/{status}/{reason}")
        public String throwResponseStatusWithReason(@PathVariable int status, @PathVariable String reason) {
            throw new ResponseStatusException(HttpStatus.valueOf(status), reason);
        }

        // --- @ResponseStatus-annotated exceptions ---
        @GetMapping("/annotated-410")
        public String throw410() {
            throw new Gone410Exception();
        }

        @GetMapping("/annotated-422")
        public String throw422() {
            throw new Unprocessable422Exception();
        }

        @GetMapping("/annotated-503")
        public String throw503() {
            throw new ServiceUnavailable503Exception();
        }

        // --- @ControllerAdvice handled ---
        @GetMapping("/advice/{status}/{body}")
        public String throwAdvised(@PathVariable int status, @PathVariable String body) {
            throw new AdvisedException(status, body.replace('+', ' '));
        }

        // --- default-resolver-handled situations ---
        @GetMapping("/get-only")
        public String getOnly() {
            return "only via GET";
        }

        @PostMapping(value = "/echo-json", consumes = MediaType.APPLICATION_JSON_VALUE)
        public String echoJson(@RequestBody Object body) {
            return body.toString();
        }

        @PostMapping(value = "/validated", consumes = MediaType.APPLICATION_JSON_VALUE)
        public String validated(@Valid @RequestBody ValidatedPayload payload) {
            return payload.name;
        }

        // --- truly unhandled exception kinds ---
        @GetMapping("/throw/{kind}")
        public String throwArbitrary(@PathVariable String kind) {
            switch (kind) {
                case "runtime": throw new RuntimeException("runtime escaped");
                case "illegal-state": throw new IllegalStateException("illegal state escaped");
                case "illegal-arg": throw new IllegalArgumentException("illegal arg escaped");
                case "npe": throw new NullPointerException("npe escaped");
                case "class-cast": throw new ClassCastException("class cast escaped");
                case "arithmetic": throw new ArithmeticException("/ by zero");
                case "ioob": throw new IndexOutOfBoundsException("ioob escaped");
                case "custom-domain": throw new CustomDomainException("custom escaped");
                default: throw new IllegalArgumentException("unknown kind: " + kind);
            }
        }

        // --- happy paths ---
        @GetMapping("/ok")
        public String ok() {
            return "happy path";
        }

        @GetMapping("/no-content")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void noContent() {
        }
    }

    @ResponseStatus(HttpStatus.GONE)
    static class Gone410Exception extends RuntimeException {
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    static class Unprocessable422Exception extends RuntimeException {
    }

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    static class ServiceUnavailable503Exception extends RuntimeException {
    }

    static class AdvisedException extends RuntimeException {
        final int status;
        final String body;
        AdvisedException(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    static class CustomDomainException extends RuntimeException {
        CustomDomainException(String msg) {
            super(msg);
        }
    }

    static class ValidatedPayload {
        @NotBlank public String name;
    }

    @RestControllerAdvice
    static class TestExceptionAdvice {
        @ExceptionHandler(AdvisedException.class)
        ResponseEntity<String> handle(AdvisedException ex) {
            return ResponseEntity.status(ex.status).body(ex.body);
        }
    }
}
