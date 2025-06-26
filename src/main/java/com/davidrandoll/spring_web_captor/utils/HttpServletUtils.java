package com.davidrandoll.spring_web_captor.utils;

import com.davidrandoll.spring_web_captor.event.RequestBodyPayload;
import com.davidrandoll.spring_web_captor.publisher.request.CachedBodyHttpServletRequest;
import com.davidrandoll.spring_web_captor.publisher.response.CachedBodyHttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@UtilityClass
public class HttpServletUtils {
    public static final String DEFAULT_IP = "0.0.0.0";

    public CachedBodyHttpServletRequest toCachedBodyHttpServletRequest(@NonNull HttpServletRequest request, ObjectMapper mapper) {
        if (request instanceof CachedBodyHttpServletRequest cachedBodyHttpServletRequest)
            return cachedBodyHttpServletRequest;
        return new CachedBodyHttpServletRequest(request, mapper);
    }

    public CachedBodyHttpServletResponse toCachedBodyHttpServletResponse(@NonNull HttpServletResponse response, CachedBodyHttpServletRequest requestWrapper, ObjectMapper mapper) {
        if (response instanceof CachedBodyHttpServletResponse cachedBodyHttpServletResponse)
            return cachedBodyHttpServletResponse;
        return new CachedBodyHttpServletResponse(response, requestWrapper, mapper);
    }

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR",
            "X-Real-IP"
    };

    @NonNull
    public static String getClientIpAddressIfServletRequestExist() {
        HttpServletRequest request = getCurrentHttpRequest();
        if (request == null) return DEFAULT_IP;
        String ip = null;
        for (String header : IP_HEADER_CANDIDATES) {
            String ipList = request.getHeader(header);
            if (ipList != null && !ipList.isEmpty() && !"unknown".equalsIgnoreCase(ipList)) {
                ip = ipList.split(",")[0];
                break;
            }
        }
        if (ip == null) ip = request.getRemoteAddr();
        if (ip != null && ip.equals("0:0:0:0:0:0:0:1")) ip = "127.0.0.1";

        return Optional.ofNullable(ip)
                .map(String::trim)
                .orElse(DEFAULT_IP);
    }

    @Nullable
    public static HttpServletRequest getCurrentHttpRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) return null;

        return ((ServletRequestAttributes) attributes).getRequest();
    }

    public static String normalizedUrl(String url) {
        if (url == null) return null;
        return url.replaceAll("/$", "");
    }

    public static RequestBodyPayload parseByteArrayToJsonNode(CachedBodyHttpServletRequest requestWrapper, byte[] cachedBody, ObjectMapper objectMapper) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        var contentType = requestWrapper.getContentType();
        Charset charSet = getCharsetFromContentType(contentType);
        if (contentType != null) {
            try {
                if (contentType.contains("json") && !ObjectUtils.isEmpty(cachedBody)) {
                    return new RequestBodyPayload(objectMapper.readTree(cachedBody));
                } else if (contentType.contains("multipart")) {
                    return parseMultiPartRequest(requestWrapper);
                } else if (contentType.contains("x-www-form-urlencoded")) {
                    return parseFormUrlEncodedBody(requestWrapper);
                }
            } catch (IOException e) {
                //ignore parsing errors, fallback to text node
            }
        }

        if (ObjectUtils.isEmpty(cachedBody))
            return new RequestBodyPayload(factory.nullNode());

        // if the content type is not JSON, we can try to parse it as a text node
        var stringBody = new String(cachedBody, charSet);
        return new RequestBodyPayload(factory.textNode(stringBody));
    }

    public static RequestBodyPayload parseMultiPartRequest(CachedBodyHttpServletRequest requestWrapper) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        // IMPORTANT: Need to do requestWrapper.getRequest() instead of directly using the wrapper
        // using just the wrapper won't get the files for some reason but the .getRequest does
        MultipartHttpServletRequest multipartRequest = toMultipartHttpServletRequest(requestWrapper.getRequest());

        ObjectNode objectNode = factory.objectNode();
        multipartRequest.getParameterMap().forEach((key, values) -> {
            if (values.length == 1) {
                objectNode.put(key, values[0]);
            } else {
                ArrayNode arrayNode = factory.arrayNode();
                for (String val : values) {
                    arrayNode.add(val);
                }
                objectNode.set(key, arrayNode);
            }
        });

        return new RequestBodyPayload(objectNode, multipartRequest.getMultiFileMap());
    }

    public static RequestBodyPayload parseFormUrlEncodedBody(CachedBodyHttpServletRequest requestWrapper) throws IOException {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode objectNode = factory.objectNode();
        FormHttpMessageConverter converter = new FormHttpMessageConverter();
        HttpInputMessage inputMessage = toHttpInputMessage(requestWrapper.getRequest());
        MultiValueMap<String, String> formData = converter.read(null, inputMessage);
        formData.forEach((key, values) -> {
            if (values.size() == 1) {
                objectNode.put(key, values.getFirst());
            } else {
                ArrayNode arrayNode = factory.arrayNode();
                for (String value : values) {
                    arrayNode.add(value);
                }
                objectNode.set(key, arrayNode);
            }
        });
        return new RequestBodyPayload(objectNode);
    }

    @NonNull
    private static MultipartHttpServletRequest toMultipartHttpServletRequest(ServletRequest request) {
        return switch (request) {
            case MultipartHttpServletRequest multipartHttpServletRequest -> multipartHttpServletRequest;
            case HttpServletRequest httpServletRequest -> new StandardMultipartHttpServletRequest(httpServletRequest);
            default ->
                    throw new IllegalArgumentException("Request must be an instance of HttpServletRequest or MultipartHttpServletRequest");
        };
    }

    @NonNull
    private static HttpInputMessage toHttpInputMessage(ServletRequest request) {
        return switch (request) {
            case HttpInputMessage httpInputMessage -> httpInputMessage;
            case HttpServletRequest httpServletRequest -> new ServletServerHttpRequest(httpServletRequest);
            default ->
                    throw new IllegalArgumentException("Request must be an instance of HttpServletRequest or HttpInputMessage");
        };
    }

    private static Charset getCharsetFromContentType(String contentType) {
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