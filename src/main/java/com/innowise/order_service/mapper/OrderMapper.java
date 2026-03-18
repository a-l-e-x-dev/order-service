package com.innowise.order_service.mapper;


import com.innowise.order_service.dto.OrderResponse;
import com.innowise.order_service.entity.Order;
import com.innowise.order_service.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    OrderResponse toDto(Order order);

    @Mapping(source = "item.id", target = "itemId")
    @Mapping(source = "item.name", target = "itemName")
    @Mapping(source = "item.price", target = "price")
    OrderResponse.OrderItemResponse toOrderItemDto(OrderItem orderItem);
}