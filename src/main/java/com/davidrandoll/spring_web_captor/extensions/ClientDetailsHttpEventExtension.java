package com.davidrandoll.spring_web_captor.extensions;

import com.davidrandoll.spring_web_captor.event.HttpRequestEvent;
import com.davidrandoll.spring_web_captor.event.HttpResponseEvent;
import com.davidrandoll.spring_web_captor.utils.HttpServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * This class is responsible for extending HTTP events with client details such as IP address and user agent.
 * It implements the IHttpEventExtension interface to provide additional information for both request and response events.
 */
@Slf4j
@Component("clientDetailsHttpEventExtension")
@RequiredArgsConstructor
@ConditionalOnMissingBean(name = "clientDetailsHttpEventExtension", ignored = {ClientDetailsHttpEventExtension.class})
public class ClientDetailsHttpEventExtension implements IHttpEventExtension {
    private static final String USER_IP = "userIp";
    private static final String USER_AGENT = "userAgent";
    private static final Map<String, Object> defaultResponse = Map.of(
            USER_IP, "Unknown",
            USER_AGENT, "Unknown"
    );

    @Override
    public Map<String, Object> enrichRequestEvent(HttpServletRequest req, HttpServletResponse res, HttpRequestEvent event) {
        return getClientDetails(req);
    }

    @Override
    public Map<String, Object> enrichResponseEvent(HttpServletRequest req, HttpServletResponse res, HttpRequestEvent reqEvent, HttpResponseEvent resEvent) {
        return getClientDetails(req);
    }

    private Map<String, Object> getClientDetails(HttpServletRequest request) {
        try {
            var ipAddress = HttpServletUtils.getClientIpAddressIfServletRequestExist(request);
            var userAgent = Optional.ofNullable(request.getHeader("User-Agent"))
                    .orElse("Unknown");
            return Map.of(
                    USER_IP, ipAddress,
                    USER_AGENT, userAgent
            );
        } catch (Exception e) {
            log.error("Error getting client details", e);
            return defaultResponse;
        }
    }
}
