package com.davidrandoll.spring_web_captor.publisher.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.HashMap;

@Slf4j
@RequiredArgsConstructor
public class RuntimeExceptionResolver implements HandlerExceptionResolver {
    private final ObjectMapper mapper;
    private final ErrorProperties errorProperties;

    @Override
    public ModelAndView resolveException(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, Object handler, @NonNull Exception ex) {
        if (ex instanceof RuntimeException) {
            log.error("Unhandled runtime exception", ex);
            try {
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
                return new ModelAndView(); // marks it resolved
            } catch (IOException e) {
                // log or ignore
            }
        }
        return null; // let other resolvers handle it
    }
}
