package com.davidrandoll.spring_web_captor.body_parser.registry;

import com.davidrandoll.spring_web_captor.body_parser.IBodyParser;
import com.davidrandoll.spring_web_captor.event.BodyPayload;
import jakarta.servlet.ServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.OrderComparator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class AbstractBodyParserRegistry implements IBodyParserRegistry {
    protected final List<IBodyParser> parsers = new ArrayList<>();

    @Override
    public void register(IBodyParser parser) {
        parsers.add(parser);
    }

    protected List<IBodyParser> getParsers() {
        return parsers.stream()
                .sorted(OrderComparator.INSTANCE)
                .toList();
    }

    @Override
    public BodyPayload parse(ServletRequest request, byte[] body) {
        String contentType = request.getContentType();
        for (IBodyParser parser : this.getParsers()) {
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
}