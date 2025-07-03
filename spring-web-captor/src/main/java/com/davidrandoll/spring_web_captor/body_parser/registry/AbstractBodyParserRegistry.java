package com.davidrandoll.spring_web_captor.body_parser.registry;

import com.davidrandoll.spring_web_captor.body_parser.IRequestBodyParser;
import com.davidrandoll.spring_web_captor.body_parser.IResponseBodyParser;
import com.davidrandoll.spring_web_captor.event.BodyPayload;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.OrderComparator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class AbstractBodyParserRegistry implements IBodyParserRegistry {
    protected final List<IRequestBodyParser> requestBodyParsers = new ArrayList<>();
    protected final List<IResponseBodyParser> responseBodyParsers = new ArrayList<>();

    @Override
    public void register(IRequestBodyParser parser) {
        this.requestBodyParsers.add(parser);
    }

    @Override
    public void register(IResponseBodyParser parser) {
        this.responseBodyParsers.add(parser);
    }

    protected List<IRequestBodyParser> getRequestParsers() {
        var sortedList = requestBodyParsers.stream()
                .sorted(OrderComparator.INSTANCE)
                .toList();
        return sortedList;
    }

    protected List<IResponseBodyParser> getResponseBodyParsers() {
        return responseBodyParsers.stream()
                .sorted(OrderComparator.INSTANCE)
                .toList();
    }

    @Override
    public BodyPayload parseRequest(ServletRequest request, byte[] body) {
        String contentType = request.getContentType();
        for (IRequestBodyParser parser : this.getRequestParsers()) {
            if (parser.supports(contentType)) {
                try {
                    return parser.parse(request, body);
                } catch (IOException e) {
                    log.error("Error parsing body with parser: {}", parser.getClass().getName(), e);
                    // ignore parsing errors, let the default parser handle it
                }
            }
        }
        throw new IllegalStateException("No parser found for content type: " + contentType);
    }

    @Override
    public BodyPayload parseResponse(HttpServletResponse response, byte[] body) {
        String contentType = response.getContentType();
        for (IResponseBodyParser parser : this.getResponseBodyParsers()) {
            if (parser.supports(contentType)) {
                try {
                    return parser.parse(response, body);
                } catch (IOException e) {
                    log.error("Error parsing response body with parser: {}", parser.getClass().getName(), e);
                    // ignore parsing errors, let the default parser handle it
                }
            }
        }
        throw new IllegalStateException("No parser found for content type: " + contentType);
    }
}