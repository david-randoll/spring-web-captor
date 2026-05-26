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
import java.util.List;

/**
 * Safety-net renderer for runtime exceptions that escape <em>everything</em> — no
 * {@code HandlerExceptionResolver} claimed them inside the dispatcher, no servlet filter
 * outside the dispatcher (e.g. Spring Security's {@code ExceptionTranslationFilter})
 * translated them via {@code sendError}, and there is no Spring Boot {@code /error}
 * endpoint that would render them on an error dispatch.
 *
 * <p>Exceptions whose class lives in any package listed in
 * {@code web-captor.defer-outer-filter-packages} (default: Spring Security) are re-thrown
 * instead of being rendered, so the outer translator can run. To extend for another framework
 * with the same outer-filter-translates-via-sendError pattern, just add its root package to
 * the property — no code change to the library needed.
 */
@Slf4j
@RequiredArgsConstructor
public class UnhandledExceptionResponseFilter extends OncePerRequestFilter {

    private final ObjectMapper mapper;
    private final ErrorProperties errorProperties;
    /**
     * Package-name prefixes whose exceptions are deferred to an outer filter.
     * Backed by {@code web-captor.defer-outer-filter-packages}; default is
     * {@code [org.springframework.security.]}.
     */
    private final List<String> deferOuterFilterPackages;

    @Override
    protected void initFilterBean() {
        log.info("UnhandledExceptionResponseFilter initialized; defer packages: {}", deferOuterFilterPackages);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (ServletException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            handleEscapedException(request, response, cause, ex);
        } catch (RuntimeException ex) {
            handleEscapedException(request, response, ex, ex);
        }
    }

    private void handleEscapedException(HttpServletRequest request, HttpServletResponse response, Throwable cause, Exception originalToRethrow) throws IOException, ServletException {
        if (shouldDeferToOuterFilter(cause, deferOuterFilterPackages)) {
            log.debug("Deferring {} so an outer translator can handle it", cause.getClass().getName());
            rethrow(originalToRethrow);
            return;
        }
        if (response.isCommitted()) {
            log.warn("Response already committed when {} escaped; re-raising for container error handling",
                    cause.getClass().getName());
            rethrow(originalToRethrow);
            return;
        }
        log.error("Unhandled exception bubbled to outer captor filter — rendering 500. Exception type: {}",
                cause.getClass().getName(), cause);
        writeErrorBody(request, response, cause);
    }

    /**
     * Walks the exception's cause chain looking for any frame whose class name starts with one
     * of the configured defer-prefixes. Depth-limited and self-reference-safe. Public so it can
     * be used in tests / custom filters.
     */
    public static boolean shouldDeferToOuterFilter(Throwable t, List<String> packagePrefixes) {
        if (packagePrefixes == null || packagePrefixes.isEmpty()) return false;
        Throwable cursor = t;
        int depth = 0;
        while (cursor != null && depth++ < 16) {
            String className = cursor.getClass().getName();
            for (String prefix : packagePrefixes) {
                if (prefix != null && !prefix.isEmpty() && className.startsWith(prefix)) {
                    return true;
                }
            }
            Throwable next = cursor.getCause();
            if (next == cursor) break;
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
