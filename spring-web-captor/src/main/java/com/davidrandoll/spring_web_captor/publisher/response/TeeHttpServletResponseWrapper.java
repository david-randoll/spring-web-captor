package com.davidrandoll.spring_web_captor.publisher.response;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class TeeHttpServletResponseWrapper extends HttpServletResponseWrapper {
    private final ByteArrayOutputStream copy = new ByteArrayOutputStream();
    private ServletOutputStream teeStream;

    public TeeHttpServletResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (teeStream == null) {
            teeStream = new TeeServletOutputStream(super.getOutputStream(), copy);
        }
        return teeStream;
    }

    public byte[] getCapturedResponseBody() {
        return copy.toByteArray();
    }
}

