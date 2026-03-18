package com.innowise.order_service.controller;

import com.innowise.order_service.dto.OrderRequest;
import com.innowise.order_service.dto.OrderWithUserResponse;
import com.innowise.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderWithUserResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        OrderWithUserResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response); // 201 Created
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderWithUserResponse> getOrderById(@PathVariable Long id) {
        OrderWithUserResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(response); // 200 OK
    }

    @GetMapping
    public ResponseEntity<Page<OrderWithUserResponse>> getOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) List<String> statuses,
            Pageable pageable) {
        Page<OrderWithUserResponse> response = orderService.getOrders(startDate, endDate, statuses, pageable);
        return ResponseEntity.ok(response); // 200 OK
    }

    @GetMapping("/user/{email}")
    public ResponseEntity<Page<OrderWithUserResponse>> getOrdersByUserEmail(@PathVariable String email, Pageable pageable) {
        Page<OrderWithUserResponse> response = orderService.getOrdersByUserEmail(email, pageable);
        return ResponseEntity.ok(response); // 200 OK
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderWithUserResponse> updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        OrderWithUserResponse response = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(response); // 200 OK
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}