package com.innowise.order_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.order_service.client.UserServiceClient;
import com.innowise.order_service.dto.OrderRequest;
import com.innowise.order_service.dto.OrderStatusRequest;
import com.innowise.order_service.dto.UserDto;
import com.innowise.order_service.entity.Item;
import com.innowise.order_service.entity.Order;
import com.innowise.order_service.enums.OrderStatus;
import com.innowise.order_service.repository.ItemRepository;
import com.innowise.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    private Item savedItem;

    @BeforeEach
    void setUp() {
        Item item = new Item();
        item.setName("Test Smartphone");
        item.setPrice(BigDecimal.valueOf(150.0));
        savedItem = itemRepository.save(item);

        UserDto mockUser = new UserDto(1L, "test@mail.com", "John", "Doe");
        when(userServiceClient.getUserById(anyLong())).thenReturn(mockUser);
    }

    @Test
    void shouldCreateOrderAndReturnCombinedResponse() throws Exception {
        OrderRequest.OrderItemRequest itemRequest = new OrderRequest.OrderItemRequest(savedItem.getId(), 2);
        OrderRequest orderRequest = new OrderRequest(1L, List.of(itemRequest));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.userId").value(1))
                .andExpect(jsonPath("$.order.status").value(OrderStatus.CREATED.name()))
                .andExpect(jsonPath("$.order.totalPrice").value(300.0))
                .andExpect(jsonPath("$.user.firstName").value("John"));
    }

    @Test
    void shouldGetOrderById() throws Exception {
        Order order = new Order();
        order.setUserId(1L);
        order.setStatus(OrderStatus.CREATED);
        order = orderRepository.save(order);

        mockMvc.perform(get("/api/v1/orders/{id}", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.userId").value(1L))
                .andExpect(jsonPath("$.user.email").value("test@mail.com"));
    }

    @Test
    void shouldReturn404WhenOrderNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateOrderStatus() throws Exception {
        Order order = new Order();
        order.setUserId(1L);
        order.setStatus(OrderStatus.CREATED);
        order = orderRepository.save(order);

        OrderStatusRequest statusRequest = new OrderStatusRequest(OrderStatus.SHIPPED);

        mockMvc.perform(patch("/api/v1/orders/{id}/status", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.status").value(OrderStatus.SHIPPED.name()));

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.SHIPPED, updatedOrder.getStatus());
    }

    @Test
    void shouldDeleteOrder() throws Exception {
        Order order = new Order();
        order.setUserId(1L);
        order.setStatus(OrderStatus.CREATED);
        order = orderRepository.save(order);

        mockMvc.perform(delete("/api/v1/orders/{id}", order.getId()))
                .andExpect(status().isOk());

        assertEquals(0, orderRepository.count());
    }
}