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

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit-level contract for {@link UnhandledExceptionResponseFilter}.
 */
class UnhandledExceptionResponseFilterUnitTest {

    private static final List<String> SPRING_SECURITY_DEFER_PACKAGES = List.of("org.springframework.security.");

    private final ObjectMapper mapper = new ObjectMapper();
    private final UnhandledExceptionResponseFilter filter =
            new UnhandledExceptionResponseFilter(mapper, configuredErrorProperties(), SPRING_SECURITY_DEFER_PACKAGES);

    private static ErrorProperties configuredErrorProperties() {
        var props = new ErrorProperties();
        props.setIncludeMessage(ErrorProperties.IncludeAttribute.ALWAYS);
        props.setIncludePath(ErrorProperties.IncludeAttribute.ALWAYS);
        return props;
    }

    @Nested
    class PassThroughBehavior {
        @ParameterizedTest(name = "status {0} from inner chain is preserved")
        @ValueSource(ints = {200, 201, 204, 302, 400, 401, 403, 404, 418, 422, 500, 503})
        void preservesAnyStatusSetByTheInnerChain(int upstreamStatus) throws Exception {
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(upstreamStatus);

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(upstreamStatus);
        }

        @Test
        void preservesBodyFromInnerChain() throws Exception {
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> {
                MockHttpServletResponse mr = (MockHttpServletResponse) res;
                mr.setStatus(200);
                mr.getWriter().write("ok");
            };

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getContentAsString()).isEqualTo("ok");
        }
    }

    @Nested
    class FallbackBehavior {
        static Stream<RuntimeException> escapedExceptions() {
            return Stream.of(
                    new RuntimeException("plain runtime"),
                    new IllegalStateException("illegal state"),
                    new IllegalArgumentException("illegal arg"),
                    new NullPointerException("npe"),
                    new ClassCastException("cce"),
                    new ArithmeticException("/ by zero")
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
        void unwrapsServletExceptionCauseAndStillRenders500() throws Exception {
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> { throw new ServletException(new RuntimeException("real cause")); };

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(500);
            JsonNode body = mapper.readTree(response.getContentAsString());
            assertThat(body.get("message").asText()).isEqualTo("real cause");
        }
    }

    @Nested
    class SecurityDeferralBehavior {
        @Test
        void deferAccessDeniedException() {
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> { throw new AccessDeniedException("denied"); };

            assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                    .isInstanceOf(AccessDeniedException.class);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        void deferAuthenticationException() {
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> { throw new BadCredentialsException("nope"); };

            assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        void deferAuthorizationDeniedException() {
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> {
                throw new AuthorizationDeniedException("denied", new AuthorizationDecision(false));
            };

            assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                    .isInstanceOf(AuthorizationDeniedException.class);
        }

        @Test
        void deferSecurityExceptionWrappedInRuntimeException() {
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> {
                throw new RuntimeException("wrapper", new AccessDeniedException("inner"));
            };

            assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        void predicateRecognizesSecurityExceptions() {
            assertThat(UnhandledExceptionResponseFilter.shouldDeferToOuterFilter(new AccessDeniedException("x"), SPRING_SECURITY_DEFER_PACKAGES)).isTrue();
            assertThat(UnhandledExceptionResponseFilter.shouldDeferToOuterFilter(new BadCredentialsException("x"), SPRING_SECURITY_DEFER_PACKAGES)).isTrue();
            assertThat(UnhandledExceptionResponseFilter.shouldDeferToOuterFilter(new RuntimeException("plain"), SPRING_SECURITY_DEFER_PACKAGES)).isFalse();
        }
    }

    @Nested
    class ConfigurableDeferPackages {
        /**
         * The crux of the future-proof design: any framework with its own outer-filter exception
         * translation can be deferred by adding its root package to the configurable list, without
         * any code change to the captor library.
         */
        @Test
        void deferAnyExceptionFromAConfiguredCustomPackage() throws Exception {
            // Domain framework with its own outer-filter translator pattern, no relation to
            // Spring Security. Add its package to the defer list — captor re-throws it.
            var customFilter = new UnhandledExceptionResponseFilter(
                    mapper,
                    configuredErrorProperties(),
                    List.of("com.acme.security."));
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> { throw new com.acme.security.CustomDenied("forbidden"); };

            assertThatThrownBy(() -> customFilter.doFilter(request, response, chain))
                    .isInstanceOf(com.acme.security.CustomDenied.class);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        void exceptionsOutsideConfiguredPackagesAreRenderedAs500() throws Exception {
            // Same domain framework, but defer list is empty — the captor catches and renders 500.
            var customFilter = new UnhandledExceptionResponseFilter(
                    mapper,
                    configuredErrorProperties(),
                    List.of()); // no defer
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> { throw new com.acme.security.CustomDenied("forbidden"); };

            customFilter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(500);
        }

        @Test
        void multiplePackagesAreSupported() throws Exception {
            var customFilter = new UnhandledExceptionResponseFilter(
                    mapper,
                    configuredErrorProperties(),
                    List.of("org.springframework.security.", "com.acme.security."));
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> { throw new com.acme.security.CustomDenied("forbidden"); };

            assertThatThrownBy(() -> customFilter.doFilter(request, response, chain))
                    .isInstanceOf(com.acme.security.CustomDenied.class);
        }

        @Test
        void predicateAcceptsArbitraryPackagePrefixes() {
            assertThat(UnhandledExceptionResponseFilter.shouldDeferToOuterFilter(
                    new com.acme.security.CustomDenied("x"),
                    List.of("com.acme.security."))).isTrue();
            assertThat(UnhandledExceptionResponseFilter.shouldDeferToOuterFilter(
                    new RuntimeException("not-in-list"),
                    List.of("com.acme.security."))).isFalse();
        }

        @Test
        void emptyOrNullPackageListNeverDefers() {
            assertThat(UnhandledExceptionResponseFilter.shouldDeferToOuterFilter(
                    new AccessDeniedException("x"), List.of())).isFalse();
            assertThat(UnhandledExceptionResponseFilter.shouldDeferToOuterFilter(
                    new AccessDeniedException("x"), null)).isFalse();
        }
    }

    @Nested
    class CommittedResponseBehavior {
        @Test
        void rethrowsIfResponseIsCommitted() {
            var request = new MockHttpServletRequest("GET", "/x");
            var response = new MockHttpServletResponse();
            FilterChain chain = (req, res) -> {
                ((MockHttpServletResponse) res).setCommitted(true);
                throw new IllegalStateException("partial then boom");
            };

            assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
