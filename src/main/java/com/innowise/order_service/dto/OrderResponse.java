package com.innowise.order_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String userEmail,
        String status,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {
    public record OrderItemResponse(Long id, Long itemId, String itemName, Integer quantity, BigDecimal price) {
    }
}