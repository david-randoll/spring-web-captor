package com.davidrandoll.spring_web_captor.body_parser.parsers;

import com.davidrandoll.spring_web_captor.body_parser.IBodyParser;
import com.davidrandoll.spring_web_captor.event.BodyPayload;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.servlet.ServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Order(Ordered.LOWEST_PRECEDENCE)
public class TextBodyParser implements IBodyParser {
    @Override
    public boolean supports(String contentType) {
        return true; // fallback if no other parser supports it
    }


    @Override
    public BodyPayload parse(ServletRequest request, byte[] body) throws IOException {
        if (ObjectUtils.isEmpty(body))
            return new BodyPayload(JsonNodeFactory.instance.nullNode());

        Charset charset = getCharset(request.getContentType());
        String text = new String(body, charset);
        return new BodyPayload(JsonNodeFactory.instance.textNode(text));
    }

    private Charset getCharset(String contentType) {
        Charset charset = null;
        if (contentType != null) {
            try {
                MediaType mediaType = MediaType.parseMediaType(contentType);
                if (mediaType.getCharset() != null) {
                    charset = mediaType.getCharset();
                }
            } catch (Exception e) {
                // Ignore parsing errors, fallback to UTF-8
            }
        }
        return Optional.ofNullable(charset)
                .orElse(StandardCharsets.UTF_8);
    }
}
