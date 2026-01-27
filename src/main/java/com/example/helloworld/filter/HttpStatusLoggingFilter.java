package com.example.helloworld.filter;

import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import java.io.IOException;

@Component
public class HttpStatusLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpStatusLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if ("/favicon.ico".equals(request.getRequestURI()) || request.getRequestURI().startsWith("/swagger-ui")
                || request.getRequestURI().startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            double durationSeconds = duration / 1000.0;

            log.info("HTTP request processed",
                    StructuredArguments.kv("method", request.getMethod()),
                    StructuredArguments.kv("path", request.getRequestURI()),
                    StructuredArguments.kv("status_code", responseWrapper.getStatus()),
                    StructuredArguments.kv("duration_sec", durationSeconds),
                    StructuredArguments.kv("client_ip", getClientIp(requestWrapper)),
                    StructuredArguments.kv("location", getLocation(getClientIp(requestWrapper)))
            // StructuredArguments.kv("request_headers", getHeaders(requestWrapper)),
            // StructuredArguments.kv("response_headers", getHeaders(responseWrapper)),
            );

            responseWrapper.copyBodyToResponse();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String[] IP_HEADERS = {
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
                "REMOTE_ADDR"
        };

        for (String header : IP_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && value.length() != 0 && !"unknown".equalsIgnoreCase(value)) {
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String getLocation(String ip) {
        if (ip == null)
            return "Unknown";
        if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.startsWith("192.168.")
                || ip.startsWith("10.")) {
            return "Local/Internal";
        }
        return "External (GeoIP Required)";
    }

    private java.util.Map<String, String> getHeaders(HttpServletRequest request) {
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (isSensitiveHeader(headerName)) {
                headers.put(headerName, "*****");
            } else {
                headers.put(headerName, request.getHeader(headerName));
            }
        }
        return headers;
    }

    private java.util.Map<String, String> getHeaders(HttpServletResponse response) {
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        for (String headerName : response.getHeaderNames()) {
            if (isSensitiveHeader(headerName)) {
                headers.put(headerName, "*****");
            } else {
                headers.put(headerName, response.getHeader(headerName));
            }
        }
        return headers;
    }

    private boolean isSensitiveHeader(String headerName) {
        return "authorization".equalsIgnoreCase(headerName) ||
                "cookie".equalsIgnoreCase(headerName) ||
                "set-cookie".equalsIgnoreCase(headerName);
    }

    private String getBody(byte[] content, String encoding) {
        if (content == null || content.length == 0) {
            return null;
        }
        try {
            int length = Math.min(content.length, 10000); // Truncate large bodies
            return new String(content, 0, length, encoding != null ? encoding : "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return "[unsupported encoding]";
        }
    }
}