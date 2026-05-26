package com.davidrandoll.spring_web_captor.publisher.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;

/**
 * Last-resort fallback that renders a JSON 500 body for exceptions that escape
 * <em>every</em> other layer of the request pipeline.
 *
 * <p>Why we still need a security-exception skip here: depending on how Spring Security 6's
 * {@code CompositeFilterChainProxy} composes the host application's servlet filters, this
 * filter can end up <em>inside</em> {@code ExceptionTranslationFilter}'s call to
 * {@code chain.doFilter}. In that configuration, an {@code AccessDeniedException} bubbling up
 * from the dispatcher reaches us <em>before</em> {@code ExceptionTranslationFilter} can
 * translate it to a 403/401. We detect that situation structurally — by checking whether the
 * escaped exception (or any cause in its chain) is a Spring Security exception — and
 * <em>re-throw</em> it so the outer security filter can do its job.
 *
 * <p>Detection uses a <strong>package prefix</strong> (any class in
 * {@code org.springframework.security.*}), not a hard-coded class list, so new Spring Security
 * exception subclasses in future versions are handled automatically — no maintenance required.
 *
 * <p>For every other unhandled {@code RuntimeException} we render the captor's standard 500 body.
 * If a response is already committed when an exception escapes, we re-raise rather than try to
 * write a new body — the client gets whatever the container's error handling produces (never
 * an empty response).
 */
@Slf4j
@RequiredArgsConstructor
public class UnhandledExceptionResponseFilter extends OncePerRequestFilter {

    /**
     * Detection is reflection-based at class init so spring-web-captor still works in apps
     * that don't have spring-security on the classpath — these references stay null and the
     * security-skip is simply inert.
     */
    private static final Class<?> ACCESS_DENIED_CLASS = loadClass("org.springframework.security.access.AccessDeniedException");
    private static final Class<?> AUTHENTICATION_EXCEPTION_CLASS = loadClass("org.springframework.security.core.AuthenticationException");
    private static final String SECURITY_PACKAGE_PREFIX = "org.springframework.security.";

    private static Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private final ObjectMapper mapper;
    private final ErrorProperties errorProperties;

    @Override
    protected void initFilterBean() {
        log.info("UnhandledExceptionResponseFilter initialized (safety-net for truly-unhandled exceptions)");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
            if (log.isDebugEnabled()) {
                log.debug("UnhandledExceptionResponseFilter pass-through: status={} committed={}",
                        response.getStatus(), response.isCommitted());
            }
        } catch (ServletException ex) {
            // FrameworkServlet wraps anything that escapes the dispatcher (incl. unhandled
            // RuntimeExceptions) in a ServletException. Unwrap to the real cause for the body.
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            handleEscapedException(request, response, cause, ex);
        } catch (RuntimeException ex) {
            handleEscapedException(request, response, ex, ex);
        }
    }

    private void handleEscapedException(HttpServletRequest request, HttpServletResponse response, Throwable cause, Exception originalToRethrow) throws IOException, ServletException {
        // 1. Defer to Spring Security: re-throw so its ExceptionTranslationFilter
        //    (which wraps this filter in some configurations) can translate to 401/403.
        if (shouldDeferToSecurityFilter(cause)) {
            log.debug("Deferring Spring Security exception {} so ExceptionTranslationFilter can translate it",
                    cause.getClass().getName());
            rethrow(originalToRethrow);
            return; // unreachable
        }
        // 2. Response already committed: we can't write a new body. Re-raise.
        if (response.isCommitted()) {
            log.warn("Response already committed when {} escaped; re-raising for container error handling",
                    cause.getClass().getName());
            rethrow(originalToRethrow);
            return; // unreachable
        }
        // 3. Truly unhandled — render the standard 500 body so the client gets a real response.
        log.error("Unhandled exception bubbled to outer captor filter — rendering 500. Exception type: {}",
                cause.getClass().getName(), cause);
        writeErrorBody(request, response, cause);
    }

    /**
     * Walks the cause chain looking for any Spring Security exception type. Uses an
     * {@code isInstance} check against the loaded {@code AccessDeniedException} /
     * {@code AuthenticationException} classes (covers all current and future subclasses) and
     * a package-prefix fallback (covers any other security exception that doesn't extend those
     * two roots — e.g. internal token-validation exceptions in oauth2 resource-server).
     */
    public static boolean shouldDeferToSecurityFilter(Throwable t) {
        Throwable cursor = t;
        int depth = 0;
        while (cursor != null && depth++ < 16) {
            if (ACCESS_DENIED_CLASS != null && ACCESS_DENIED_CLASS.isInstance(cursor)) return true;
            if (AUTHENTICATION_EXCEPTION_CLASS != null && AUTHENTICATION_EXCEPTION_CLASS.isInstance(cursor)) return true;
            if (cursor.getClass().getName().startsWith(SECURITY_PACKAGE_PREFIX)) return true;
            Throwable next = cursor.getCause();
            if (next == cursor) break; // self-cause guard
            cursor = next;
        }
        return false;
    }

    private static void rethrow(Exception originalToRethrow) throws ServletException {
        if (originalToRethrow instanceof ServletException se) throw se;
        if (originalToRethrow instanceof RuntimeException re) throw re;
        throw new ServletException(originalToRethrow);
    }

    private void writeErrorBody(HttpServletRequest request, HttpServletResponse response, Throwable ex) throws IOException {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        var body = new HashMap<String, Object>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        if (errorProperties.getIncludePath().equals(ErrorProperties.IncludeAttribute.ALWAYS))
            body.put("path", request.getRequestURI());
        body.put("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        if (errorProperties.getIncludeMessage().equals(ErrorProperties.IncludeAttribute.ALWAYS))
            body.put("message", ex.getMessage());
        if (errorProperties.getIncludeStacktrace().equals(ErrorProperties.IncludeAttribute.ALWAYS) || errorProperties.isIncludeException())
            body.put("trace", mapper.writeValueAsString(ex.getStackTrace()));

        mapper.writeValue(response.getOutputStream(), body);
    }
}
