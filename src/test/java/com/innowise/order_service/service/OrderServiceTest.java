package com.innowise.order_service.service;

import com.innowise.order_service.client.UserServiceClient;
import com.innowise.order_service.dto.*;
import com.innowise.order_service.entity.*;
import com.innowise.order_service.mapper.OrderMapper;
import com.innowise.order_service.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private UserServiceClient userServiceClient;
    @Mock private OrderMapper orderMapper;

    @InjectMocks private OrderService orderService;

    @Test
    void createOrder_Success() {
        // Arrange
        OrderRequest request = new OrderRequest("test@mail.com", List.of(new OrderRequest.OrderItemRequest(1L, 2)));
        Item item = new Item();
        item.setId(1L);
        item.setPrice(BigDecimal.valueOf(100));

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setUserEmail("test@mail.com");

        UserDto userDto = new UserDto(1L, "test@mail.com", "John", "Doe");
        OrderResponse orderResponse = new OrderResponse(1L, "test@mail.com", "CREATED", BigDecimal.valueOf(200), null, null);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderResponse);
        when(userServiceClient.getUserByEmail("test@mail.com")).thenReturn(userDto);

        // Act
        OrderWithUserResponse result = orderService.createOrder(request);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(200), result.order().totalPrice());
        assertEquals("John", result.user().firstName());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void getOrderById_Success() {
        Order order = new Order();
        order.setUserEmail("test@mail.com");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toDto(order)).thenReturn(new OrderResponse(1L, "test@mail.com", "CREATED", BigDecimal.TEN, null, null));
        when(userServiceClient.getUserByEmail("test@mail.com")).thenReturn(new UserDto(1L, "test@mail.com", "John", "Doe"));

        OrderWithUserResponse result = orderService.getOrderById(1L);

        assertNotNull(result);
        verify(orderRepository).findById(1L);
    }

    @Test
    void getOrderById_NotFound_ThrowsException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> orderService.getOrderById(1L));
    }

    @Test
    void getOrdersByUserEmail_Success() {
        Page<Order> page = new PageImpl<>(List.of(new Order()));
        when(orderRepository.findAllByUserEmail(eq("test@mail.com"), any(PageRequest.class))).thenReturn(page);

        Page<OrderWithUserResponse> result = orderService.getOrdersByUserEmail("test@mail.com", PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void updateOrderStatus_Success() {
        Order order = new Order();
        order.setStatus("CREATED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.updateOrderStatus(1L, "SHIPPED");

        assertEquals("SHIPPED", order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void deleteOrder_Success() {
        when(orderRepository.existsById(1L)).thenReturn(true);
        orderService.deleteOrder(1L);
        verify(orderRepository).deleteById(1L);
    }
}