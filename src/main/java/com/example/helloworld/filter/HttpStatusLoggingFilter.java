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
import java.io.IOException;

@Component
public class HttpStatusLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpStatusLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if ("/favicon.ico".equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return; // Skip the log.info part
        }
        // 1. Capture the start time in milliseconds
        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 2. Calculate duration and convert to seconds (double)
            double durationSeconds = (System.currentTimeMillis() - startTime) / 1000.0;

            // 3. Log using StructuredArguments
            log.info("HTTP request processed: {} {}", 
                request.getMethod(), 
                request.getRequestURI(),
                StructuredArguments.kv("method", request.getMethod()),
                StructuredArguments.kv("path", request.getRequestURI()),
                StructuredArguments.kv("status_code", response.getStatus()),
                // This will now output 10.062 instead of 10062
                StructuredArguments.kv("duration_sec", durationSeconds) 
            );
        }
    }
}