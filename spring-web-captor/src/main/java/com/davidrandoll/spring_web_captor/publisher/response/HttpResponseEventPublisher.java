package com.davidrandoll.spring_web_captor.publisher.response;

import com.davidrandoll.spring_web_captor.publisher.IHttpEventPublisher;
import com.davidrandoll.spring_web_captor.publisher.request.CachedBodyHttpServletRequest;
import com.davidrandoll.spring_web_captor.publisher.request.HttpRequestEventPublisher;
import com.davidrandoll.spring_web_captor.utils.HttpServletUtils;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Slf4j
@RequiredArgsConstructor
public class HttpResponseEventPublisher extends OncePerRequestFilter {
    private final IHttpEventPublisher publisher;

    /**
     * Run on error dispatches too. When a handler calls {@code response.sendError(...)} (this is
     * what {@code ExceptionTranslationFilter} and {@code ResponseStatusExceptionResolver} do),
     * Tomcat re-invokes the filter chain with {@link DispatcherType#ERROR} so {@code /error}'s
     * controller can render the final body. With the default {@code shouldNotFilterErrorDispatch=true}
     * we'd miss that — the network log row would have a captured request but null response body.
     * Running on the error dispatch lets us capture the rendered response.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    /**
     * NOTE: Cannot publish the request event here because the path params are not available here yet.
     * After the filter chain is executed, the path params are available in the requestWrapper object.
     * This is why in the {@link  HttpRequestEventPublisher#preHandle}, the event is published in the preHandle method.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws IOException, ServletException {
        CachedBodyHttpServletRequest requestWrapper = HttpServletUtils.toCachedBodyHttpServletRequest(request);
        CachedBodyHttpServletResponse responseWrapper = HttpServletUtils.toCachedBodyHttpServletResponse(response, requestWrapper);

        boolean isErrorDispatch = request.getDispatcherType() == DispatcherType.ERROR;
        boolean shouldPublishRequest = publisher.shouldPublishRequestEvent(requestWrapper, responseWrapper);

        log.debug("HttpResponseEventPublisher: dispatch={} path={}", request.getDispatcherType(), request.getRequestURI());

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);

            // On the original dispatch, fallback-publish the request if the interceptor didn't.
            // On the error dispatch, skip — the original dispatch already published this.
            if (!isErrorDispatch && !requestWrapper.isPublished() && shouldPublishRequest) {
                publisher.publishRequestEvent(requestWrapper, responseWrapper);
            }
            if (publisher.shouldPublishResponseEvent(requestWrapper, responseWrapper)) {
                log.debug("HttpResponseEventPublisher: publishing response on dispatch={} status={}",
                        request.getDispatcherType(), response.getStatus());
                responseWrapper.getResponseBody()
                        .thenRun(() -> publisher.publishResponseEvent(requestWrapper, responseWrapper))
                        .exceptionally(ex -> {
                            log.error("Failed to publish response event", ex);
                            return null;
                        });
            }
        } catch (Exception ex) {
            responseWrapper.getResponseBody()
                    .completeExceptionally(ex);
            // Publish the request if we have it (so the network log has a row); intentionally do
            // NOT publish the response here. At this point the exception is still propagating up
            // toward outer filters (e.g. Spring Security's ExceptionTranslationFilter, which will
            // call sendError) — the response status is still 200 and the body is empty. Publishing
            // now would record an incorrect status that races with — and can overwrite — the
            // correct status produced by the subsequent ERROR dispatch.
            try {
                if (!isErrorDispatch && !requestWrapper.isPublished() && shouldPublishRequest) {
                    publisher.publishRequestEvent(requestWrapper, responseWrapper);
                }
            } catch (Exception publishEx) {
                log.error("Failed to publish request event on exception path", publishEx);
            }
            throw ex;
        }
    }
}
