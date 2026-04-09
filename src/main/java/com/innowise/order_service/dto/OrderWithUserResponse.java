package com.innowise.order_service.dto;

public record OrderWithUserResponse(
        OrderResponse order,
        UserDto user
) {
}