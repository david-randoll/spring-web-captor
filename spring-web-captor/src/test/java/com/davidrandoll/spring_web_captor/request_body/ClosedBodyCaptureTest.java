package com.davidrandoll.spring_web_captor.request_body;

import com.davidrandoll.spring_web_captor.publisher.request.CachedBodyHttpServletRequest;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.*;

class ClosedBodyCaptureTest {
    @Test
    void anAlreadyClosedBodylessRequestIsCapturedAsEmpty() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletInputStream stream = mock(ServletInputStream.class);
        when(request.getInputStream()).thenReturn(stream);
        when(stream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("Stream closed"));
        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request);
        assertArrayEquals(new byte[0], wrapped.getCachedBody());
        assertArrayEquals(new byte[0], wrapped.getCachedBody());
    }
}
