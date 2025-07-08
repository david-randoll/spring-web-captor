package com.davidrandoll.spring_web_captor.publisher.response;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

import java.io.IOException;
import java.io.OutputStream;

public class TeeServletOutputStream extends ServletOutputStream {
    private final ServletOutputStream original;
    private final OutputStream copy;

    public TeeServletOutputStream(ServletOutputStream original, OutputStream copy) {
        this.original = original;
        this.copy = copy;
    }

    @Override
    public void write(int b) throws IOException {
        original.write(b);
        copy.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        original.write(b, off, len);
        copy.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        original.flush();
        copy.flush();
    }

    @Override
    public void close() throws IOException {
        original.close();
        copy.close();
    }

    @Override
    public boolean isReady() {
        return original.isReady();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        original.setWriteListener(writeListener);
    }
}