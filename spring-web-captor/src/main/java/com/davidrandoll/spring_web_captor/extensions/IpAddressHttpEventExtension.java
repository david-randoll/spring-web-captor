package com.davidrandoll.spring_web_captor.extensions;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.utils.HttpServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * This class is responsible for extending HTTP events with user ip address information.
 * It implements the IHttpEventExtension interface to provide additional information for both request and response events.
 */
@Slf4j
@RequiredArgsConstructor
public class IpAddressHttpEventExtension implements IHttpEventExtension {
    private static final String USER_IP = "userIp";

    @Override
    public Map<String, Object> enrichRequestEvent(HttpServletRequest req, HttpServletResponse res, HttpRequestEvent event) {
        return getUserIp(req);
    }

    @Override
    public Map<String, Object> enrichResponseEvent(HttpServletRequest req, HttpServletResponse res, HttpRequestEvent reqEvent, HttpResponseEvent resEvent) {
        return getUserIp(req);
    }

    private Map<String, Object> getUserIp(HttpServletRequest request) {
        try {
            var ipAddress = HttpServletUtils.getClientIpAddressIfServletRequestExist(request);
            return Map.of(USER_IP, ipAddress);
        } catch (Exception e) {
            log.error("Error getting client details", e);
            return Map.of();
        }
    }
}
