package com.davidrandoll.spring_web_captor.publisher.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.HashMap;

@RequiredArgsConstructor
public class RuntimeExceptionResolver implements HandlerExceptionResolver {
    private final ObjectMapper mapper;

    @Override
    public ModelAndView resolveException(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, Object handler, @NonNull Exception ex) {
        if (ex instanceof RuntimeException) {
            try {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                var body = new HashMap<String, Object>();
                body.put("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
                body.put("message", ex.getMessage());
                body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
                body.put("path", request.getRequestURI());
                body.put("timestamp", System.currentTimeMillis());
                mapper.writeValue(response.getWriter(), body);
                return new ModelAndView(); // marks it resolved
            } catch (IOException e) {
                // log or ignore
            }
        }
        return null; // let other resolvers handle it
    }
}
