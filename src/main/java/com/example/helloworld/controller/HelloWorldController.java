package com.example.helloworld.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api")
public class HelloWorldController {
    private static final Logger log = LoggerFactory.getLogger(HelloWorldController.class);

    private void simulateDelay() {
            try {
                Thread.sleep(1000); // 1 second blocking delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread interrupted", e);
            }
        }

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        simulateDelay();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello from AKS Deployment!");
        response.put("author", "Sudarshan");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("status", "success");
        response.put("version", "1.0.0");
        return response;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        simulateDelay();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "eSewa Application");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("details", Map.of(
                "database", "connected",
                "disk_space", "adequate",
                "memory", "healthy"));
        return response;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        simulateDelay();
        Map<String, Object> response = new HashMap<>();
        response.put("application", "eSewa - Digital Payment System");
        response.put("version", "1.0.0");
        response.put("environment", "local"); // Changed from "production"
        response.put("deployed_on", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // FIX: Handle missing environment variables
        String podName = System.getenv("HOSTNAME");
        String nodeName = System.getenv("NODE_NAME");

        Map<String, String> k8sInfo = new HashMap<>();
        k8sInfo.put("namespace", podName != null ? "esewans" : "local");
        k8sInfo.put("pod", podName != null ? podName : "localhost");
        k8sInfo.put("node", nodeName != null ? nodeName : "local-machine");

        response.put("kubernetes", k8sInfo);

        response.put("endpoints", Map.of(
                "hello", "/api/hello",
                "health", "/api/health",
                "info", "/api/info",
                "users", "/api/users",
                "transactions", "/api/transactions"));
        return response;
    }

    @GetMapping("/users")
    public Map<String, Object> getUsers() {
        simulateDelay();
        Map<String, Object> response = new HashMap<>();
        response.put("users", java.util.List.of(
                Map.of("id", 1, "name", "Sudarshan Uprety", "email", "sudarshan@esewa.com", "balance", 15000.50),
                Map.of("id", 2, "name", "John Doe", "email", "john@example.com", "balance", 5000.75),
                Map.of("id", 3, "name", "Jane Smith", "email", "jane@example.com", "balance", 12000.25)));
        response.put("total_users", 3);
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return response;
    }

    @GetMapping("/transactions")
    public Map<String, Object> getTransactions() {
        simulateDelay();
        Map<String, Object> response = new HashMap<>();
        response.put("transactions", java.util.List.of(
                Map.of("id", "TXN001", "from", "Sudarshan", "to", "John", "amount", 1000.00, "type", "transfer",
                        "status", "completed"),
                Map.of("id", "TXN002", "from", "Jane", "to", "Electricity Board", "amount", 2500.00, "type",
                        "bill_payment", "status", "completed"),
                Map.of("id", "TXN003", "from", "John", "to", "Internet Provider", "amount", 1500.00, "type",
                        "bill_payment", "status", "pending")));
        response.put("total_transactions", 3);
        response.put("total_amount", 5000.00);
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return response;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        simulateDelay();
        Map<String, Object> response = new HashMap<>();
        response.put("application", "eSewa API");
        response.put("status", "operational");
        response.put("uptime", "99.9%");
        response.put("response_time", "45ms");
        response.put("requests_today", 1250);
        response.put("active_sessions", 45);
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return response;
    }

    @GetMapping("/test-error/{code}")
        public Map<String, Object> triggerError(@PathVariable String code) {
            switch (code) {
                case "401":
                    throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Simulated Security Alert: Unauthorized access attempt detected.");
                
                case "500":
                    throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Simulated System Alert: Critical Server Failure (NullPointerException).");
                
                case "503":
                    throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "Simulated Availability Alert: Zoho bridge testing - Service Overloaded.");
                
                case "504":
                    throw new ResponseStatusException(
                        HttpStatus.GATEWAY_TIMEOUT, "Simulated Network Alert: Gateway Timeout on upstream service.");

                default:
                    throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Simulated Error: Testing status code " + code);
            }
        }
}