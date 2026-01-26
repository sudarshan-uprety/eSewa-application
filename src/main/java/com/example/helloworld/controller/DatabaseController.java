package com.example.helloworld.controller;

import com.example.helloworld.entity.LogEntry;
import com.example.helloworld.entity.Order;
import com.example.helloworld.entity.Product;
import com.example.helloworld.entity.User;
import com.example.helloworld.repository.LogRepository;
import com.example.helloworld.repository.OrderRepository;
import com.example.helloworld.repository.ProductRepository;
import com.example.helloworld.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api")
@Transactional
public class DatabaseController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private LogRepository logRepository;

    // 1. Write to users table
    @PostMapping("/users")
    public User createUser(@Valid @RequestBody User user) {
        return userRepository.save(user);
    }

    // 2. Write to products table
    @PostMapping("/products")
    public Product createProduct(@Valid @RequestBody Product product) {
        return productRepository.save(product);
    }

    // 3. Write to orders table
    @PostMapping("/orders")
    public Order createOrder(@Valid @RequestBody Order order) {
        return orderRepository.save(order);
    }

    // 4. Write to logs table
    @PostMapping("/logs")
    public LogEntry createLog(@Valid @RequestBody LogEntry logEntry) {
        return logRepository.save(logEntry);
    }

    // 5. Search in users table (Read)
    @GetMapping("/users/search")
    @Transactional(readOnly = true)
    public List<User> searchUsers(@RequestParam String name) {
        return userRepository.findByNameContainingIgnoreCase(name);
    }

    // 6. Search in products table (Read)
    @GetMapping("/products/search")
    @Transactional(readOnly = true)
    public List<Product> searchProducts(@RequestParam String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }
}
