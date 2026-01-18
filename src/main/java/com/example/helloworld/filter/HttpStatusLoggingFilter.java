package com.example.helloworld.filter;

// 1. Add this import (requires the dependency we added to pom.xml)
import net.logstash.logback.argument.StructuredArguments; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class HttpStatusLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpStatusLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Execute the request first so we can get the resulting status code
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 2. Use StructuredArguments.kv to map keys to JSON fields
            log.info("HTTP request processed: {} {}", 
                request.getMethod(), 
                request.getRequestURI(),
                StructuredArguments.kv("method", request.getMethod()),
                StructuredArguments.kv("path", request.getRequestURI()),
                StructuredArguments.kv("status_code", response.getStatus())
            );
        }
    }
}