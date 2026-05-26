package com.davidrandoll.spring_web_captor.runtime_exception_resolver;

import com.davidrandoll.spring_web_captor.publisher.response.UnhandledExceptionResponseFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive unit-level contract for the safety-net filter.
 *
 * <p>Three behavioral invariants, each exercised broadly:
 * <ol>
 *   <li><b>Pass-through.</b> When the inner chain returns normally, the filter is a no-op:
 *       status, headers and body the inner chain wrote all reach the client unchanged,
 *       regardless of what status that is.</li>
 *   <li><b>Render 500 only when truly unhandled.</b> If — and only if — an exception
 *       escapes everything, the filter writes a JSON 500 body to the client.</li>
 *   <li><b>Never silently drop a response.</b> If the response is already committed when
 *       an exception escapes, we rethrow so the container's error handling takes over;
 *       we don't truncate, we don't produce an empty response.</li>
 * </ol>
 */
class UnhandledExceptionResponseFilterUnitTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final UnhandledExceptionResponseFilter filter =
            new UnhandledExceptionResponseFilter(mapper, configuredErrorProperties());

    private static ErrorProperties configuredErrorProperties() {
        var props = new ErrorProperties();
        props.setIncludeMessage(ErrorProperties.IncludeAttribute.ALWAYS);
        props.setIncludePath(ErrorProperties.IncludeAttribute.ALWAYS);
        return props;
    }

    // ---------- Pass-through: inner chain succeeds ----------

    @Nested
    class PassThroughBehavior {

        @ParameterizedTest(name = "status {0} from inner chain is preserved")
        @ValueSource(ints = {
                200, 201, 202, 204, 206,
                301, 302, 303, 304, 307, 308,
                400, 401, 402, 403, 404, 405, 406, 408, 409, 410, 411, 412, 413, 415, 418, 422, 423, 424, 428, 429,
                500, 501, 502, 503, 504, 505, 507, 511
        })
        void preservesAnyStatusSetByTheInnerChain(int upstreamStatus) throws Exception {
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(upstreamStatus);

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(upstreamStatus);
            assertThat(response.getContentAsString()).isEmpty();
        }

        @ParameterizedTest(name = "body \"{0}\" from inner chain reaches the client unchanged")
        @ValueSource(strings = {
                "ok",
                "{\"foo\":\"bar\"}",
                "short and stout",
                "<html><body>HTML body</body></html>",
                ""
        })
        void preservesAnyBodyWrittenByTheInnerChain(String upstreamBody) throws Exception {
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> {
                MockHttpServletResponse mr = (MockHttpServletResponse) res;
                mr.setStatus(200);
                mr.getWriter().write(upstreamBody);
            };

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getContentAsString()).isEqualTo(upstreamBody);
        }

        @Test
        void preservesAnyHeaderWrittenByTheInnerChain() throws Exception {
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> {
                MockHttpServletResponse mr = (MockHttpServletResponse) res;
                mr.setHeader("X-Custom", "captor-untouched");
                mr.setContentType("application/json");
                mr.setStatus(201);
            };

            filter.doFilter(request, response, chain);

            assertThat(response.getHeader("X-Custom")).isEqualTo("captor-untouched");
            assertThat(response.getContentType()).isEqualTo("application/json");
            assertThat(response.getStatus()).isEqualTo(201);
        }

        @Test
        void preservesA403ResponseThatLooksLikeWhatSpringSecurityProduces() throws Exception {
            // Simulating ExceptionTranslationFilter (or any filter) having translated an
            // exception to 403 with a JSON body BEFORE our filter sees the response.
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> {
                MockHttpServletResponse mr = (MockHttpServletResponse) res;
                mr.setStatus(403);
                mr.setContentType("application/json");
                mr.getWriter().write("{\"error\":\"Forbidden\"}");
            };

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(response.getContentAsString()).isEqualTo("{\"error\":\"Forbidden\"}");
        }
    }

    // ---------- Render 500 when an exception escapes ----------

    @Nested
    class FallbackBehavior {

        static Stream<RuntimeException> escapedExceptions() {
            // NOTE: Spring Security exception types are intentionally excluded from this list
            // because the filter defers them (re-throws) — they're tested separately in
            // SecurityDeferralBehavior. Everything below is a non-security RuntimeException
            // that the filter is supposed to absorb and convert into a 500 body.
            return Stream.of(
                    new RuntimeException("plain runtime"),
                    new IllegalStateException("illegal state"),
                    new IllegalArgumentException("illegal arg"),
                    new NullPointerException("npe"),
                    new ClassCastException("cce"),
                    new ArithmeticException("/ by zero"),
                    new IndexOutOfBoundsException("oob"),
                    new UnsupportedOperationException("unsupported"),
                    new NumberFormatException("nan"),
                    new CustomBusinessException("custom domain exception")
            );
        }

        @ParameterizedTest(name = "{0} that escapes the chain produces a 500 with body")
        @MethodSource("escapedExceptions")
        void rendersA500BodyForAnyEscapedRuntimeException(RuntimeException ex) throws Exception {
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> { throw ex; };

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(500);
            JsonNode body = mapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(500);
            assertThat(body.get("error").asText()).isEqualTo("Internal Server Error");
        }

        @Test
        void rendered500BodyIncludesTimestamp() throws Exception {
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            long before = System.currentTimeMillis();
            FilterChain chain = (req, res) -> { throw new RuntimeException("boom"); };

            filter.doFilter(request, response, chain);

            long after = System.currentTimeMillis();
            JsonNode body = mapper.readTree(response.getContentAsString());
            assertThat(body.has("timestamp")).isTrue();
            long ts = body.get("timestamp").asLong();
            assertThat(ts).isBetween(before, after);
        }

        @Test
        void rendered500BodyIncludesPathWhenConfigured() throws Exception {
            var request = new MockHttpServletRequest("GET", "/some/specific/path");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> { throw new RuntimeException("boom"); };

            filter.doFilter(request, response, chain);

            JsonNode body = mapper.readTree(response.getContentAsString());
            assertThat(body.get("path").asText()).isEqualTo("/some/specific/path");
        }

        @Test
        void rendered500BodyIncludesMessageWhenConfigured() throws Exception {
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> { throw new RuntimeException("the message"); };

            filter.doFilter(request, response, chain);

            JsonNode body = mapper.readTree(response.getContentAsString());
            assertThat(body.get("message").asText()).isEqualTo("the message");
        }

        @Test
        void rendered500BodyOmitsMessageWhenIncludeMessageIsNever() throws Exception {
            // Default ErrorProperties has includeMessage=NEVER.
            var defaultProps = new ErrorProperties();
            var defaultFilter = new UnhandledExceptionResponseFilter(mapper, defaultProps);
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> { throw new RuntimeException("a sensitive message"); };

            defaultFilter.doFilter(request, response, chain);

            JsonNode body = mapper.readTree(response.getContentAsString());
            assertThat(body.has("message")).as("default config must not leak the exception message").isFalse();
            // ...but status and error reason phrase still present.
            assertThat(body.get("status").asInt()).isEqualTo(500);
            assertThat(body.get("error").asText()).isEqualTo("Internal Server Error");
        }

        @Test
        void rendered500BodyHasJsonContentType() throws Exception {
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> { throw new RuntimeException("boom"); };

            filter.doFilter(request, response, chain);

            assertThat(response.getContentType()).isEqualTo("application/json");
        }

        @Test
        void rendered500BodyIsActuallyWrittenToTheStreamNotJustHeldInABuffer() throws Exception {
            // The body bytes must actually be on the response output stream, not in some internal
            // field. MockHttpServletResponse.getContentAsByteArray() reads from the underlying
            // ByteArrayOutputStream that backs getOutputStream() — proving real write-through.
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> { throw new RuntimeException("boom"); };

            filter.doFilter(request, response, chain);

            byte[] raw = response.getContentAsByteArray();
            assertThat(raw).isNotEmpty();
            assertThat(new String(raw)).isEqualTo(response.getContentAsString());
        }
    }

    // ---------- ServletException unwrapping ----------

    @Nested
    class ServletExceptionUnwrapping {

        @Test
        void unwrapsServletExceptionCauseAndStillRenders500() throws Exception {
            // Spring's FrameworkServlet wraps unhandled exceptions from the dispatcher in a
            // ServletException. Our filter must catch and unwrap, not let the wrapper escape.
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            RuntimeException cause = new RuntimeException("real cause");
            FilterChain chain = (req, res) -> { throw new ServletException(cause); };

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(500);
            JsonNode body = mapper.readTree(response.getContentAsString());
            assertThat(body.get("message").asText()).isEqualTo("real cause");
        }

        @Test
        void servletExceptionWithoutACauseStillRenders500() throws Exception {
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> { throw new ServletException("no cause"); };

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(500);
        }
    }

    // ---------- Don't drop responses ----------

    @Nested
    class CommittedResponseBehavior {

        @Test
        void rethrowsRuntimeExceptionIfResponseIsCommitted() {
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> {
                ((MockHttpServletResponse) res).setCommitted(true);
                throw new IllegalStateException("partial then boom");
            };

            assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("partial then boom");
        }

        @Test
        void rethrowsServletExceptionIfResponseIsCommitted() {
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> {
                ((MockHttpServletResponse) res).setCommitted(true);
                throw new ServletException("partial then boom");
            };

            assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                    .isInstanceOf(ServletException.class)
                    .hasMessage("partial then boom");
        }
    }

    // ---------- Security exception deferral ----------

    /**
     * In some Spring Security 6 filter compositions (e.g. when {@code CompositeFilterChainProxy}
     * puts application filters inside {@code ExceptionTranslationFilter}'s call to
     * {@code chain.doFilter}), an {@code AccessDeniedException} bubbling up from the dispatcher
     * reaches this filter <em>before</em> {@code ExceptionTranslationFilter} sees it. Catching
     * it as a generic 500 would defeat Spring Security's 401/403 translation. Instead we re-throw
     * so the outer security filter does its job.
     */
    @Nested
    class SecurityDeferralBehavior {

        @Test
        void deferAccessDeniedExceptionByRethrowing() {
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> { throw new AccessDeniedException("denied"); };

            assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                    .as("AccessDeniedException must propagate so ExceptionTranslationFilter can translate to 403")
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("denied");
            assertThat(response.getStatus()).isEqualTo(200); // filter did not touch the response
            assertThat(response.getContentAsByteArray()).isEmpty();
        }

        @Test
        void deferAuthenticationExceptionByRethrowing() throws Exception {
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> { throw new BadCredentialsException("nope"); };

            assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                    .as("AuthenticationException must propagate so security can translate it")
                    .isInstanceOf(BadCredentialsException.class);
            assertThat(response.getContentAsByteArray()).isEmpty();
        }

        @Test
        void deferAuthorizationDeniedException_thePreAuthorizeSubclass() {
            // The exact subclass thrown by @PreAuthorize / @PostAuthorize via
            // AuthorizationManagerBeforeMethodInterceptor in Spring Security 6.
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> {
                throw new AuthorizationDeniedException("denied", new AuthorizationDecision(false));
            };

            assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                    .isInstanceOf(AuthorizationDeniedException.class);
            assertThat(response.getContentAsByteArray()).isEmpty();
        }

        @Test
        void deferSecurityExceptionEvenWhenWrappedInServletException() {
            // FrameworkServlet wraps escaping exceptions in ServletException. Our cause-chain
            // inspection unwraps and still defers to Spring Security.
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> {
                throw new ServletException(new AccessDeniedException("inside servlet ex"));
            };

            assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                    .isInstanceOf(ServletException.class);
            assertThat(response.getContentAsByteArray()).isEmpty();
        }

        @Test
        void deferSecurityExceptionEvenWhenWrappedInGenericRuntimeException() {
            // Some interceptors / aspects re-wrap exceptions. Cause-chain walk still finds it.
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> {
                throw new RuntimeException("wrapper", new AccessDeniedException("real cause"));
            };

            assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                    .isInstanceOf(RuntimeException.class);
            assertThat(response.getContentAsByteArray()).isEmpty();
        }

        @Test
        void shouldDeferToSecurityFilter_recognizesAccessDeniedException() {
            assertThat(UnhandledExceptionResponseFilter.shouldDeferToSecurityFilter(
                    new AccessDeniedException("x"))).isTrue();
        }

        @Test
        void shouldDeferToSecurityFilter_recognizesAuthenticationException() {
            assertThat(UnhandledExceptionResponseFilter.shouldDeferToSecurityFilter(
                    new BadCredentialsException("x"))).isTrue();
        }

        @Test
        void shouldDeferToSecurityFilter_recognizesAuthorizationDeniedException() {
            assertThat(UnhandledExceptionResponseFilter.shouldDeferToSecurityFilter(
                    new AuthorizationDeniedException("x", new AuthorizationDecision(false)))).isTrue();
        }

        @Test
        void shouldDeferToSecurityFilter_recognizesNestedSecurityExceptionInCauseChain() {
            var ex = new RuntimeException("outer",
                    new IllegalStateException("middle",
                            new AccessDeniedException("inner security")));
            assertThat(UnhandledExceptionResponseFilter.shouldDeferToSecurityFilter(ex)).isTrue();
        }

        @Test
        void shouldDeferToSecurityFilter_doesNotMatchPlainRuntimeException() {
            assertThat(UnhandledExceptionResponseFilter.shouldDeferToSecurityFilter(
                    new RuntimeException("plain"))).isFalse();
            assertThat(UnhandledExceptionResponseFilter.shouldDeferToSecurityFilter(
                    new IllegalArgumentException("plain"))).isFalse();
            assertThat(UnhandledExceptionResponseFilter.shouldDeferToSecurityFilter(
                    new NullPointerException())).isFalse();
        }

        @Test
        void shouldDeferToSecurityFilter_handlesSelfReferentialCauseSafely() {
            // Build a Throwable whose getCause() returns itself, simulating a pathological
            // exception that would otherwise infinite-loop the cause walk. The filter's depth
            // limit and self-reference guard keep this safe.
            class SelfReferencingException extends RuntimeException {
                SelfReferencingException(String msg) { super(msg); }
                @Override public synchronized Throwable getCause() { return this; }
            }
            assertThat(UnhandledExceptionResponseFilter.shouldDeferToSecurityFilter(
                    new SelfReferencingException("plain"))).isFalse();
        }
    }

    static class CustomBusinessException extends RuntimeException {
        CustomBusinessException(String msg) {
            super(msg);
        }
    }
}
