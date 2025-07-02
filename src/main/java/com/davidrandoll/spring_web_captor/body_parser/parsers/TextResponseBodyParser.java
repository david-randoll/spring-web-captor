package com.davidrandoll.spring_web_captor.body_parser.parsers;

import com.davidrandoll.spring_web_captor.body_parser.IResponseBodyParser;
import com.davidrandoll.spring_web_captor.event.BodyPayload;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.nio.charset.Charset;

import static com.davidrandoll.spring_web_captor.utils.HttpServletUtils.getCharset;

@Order(Ordered.LOWEST_PRECEDENCE)
public class TextResponseBodyParser implements IResponseBodyParser {
    @Override
    public boolean supports(String contentType) {
        return true; // fallback if no other parser supports it
    }

    @Override
    public BodyPayload parse(HttpServletResponse response, byte[] body) throws IOException {
        if (ObjectUtils.isEmpty(body))
            return new BodyPayload(JsonNodeFactory.instance.nullNode());

        Charset charset = getCharset(response.getContentType());
        String text = new String(body, charset);
        return new BodyPayload(JsonNodeFactory.instance.textNode(text));
    }
}