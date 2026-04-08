package com.innowise.order_service.service;

import com.innowise.order_service.client.UserServiceClient;
import com.innowise.order_service.dto.OrderRequest;
import com.innowise.order_service.dto.OrderResponse;
import com.innowise.order_service.dto.OrderWithUserResponse;
import com.innowise.order_service.dto.UserDto;
import com.innowise.order_service.entity.Item;
import com.innowise.order_service.entity.Order;
import com.innowise.order_service.entity.OrderItem;
import com.innowise.order_service.enums.OrderStatus;
import com.innowise.order_service.mapper.OrderMapper;
import com.innowise.order_service.repository.ItemRepository;
import com.innowise.order_service.repository.OrderRepository;
import com.innowise.order_service.repository.OrderSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final UserServiceClient userServiceClient;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderWithUserResponse createOrder(OrderRequest request) {
        Order order = new Order();
        order.setUserId(request.userId());
        order.setStatus(OrderStatus.CREATED);

        List<Long> itemIds = request.items().stream()
                .map(OrderRequest.OrderItemRequest::itemId)
                .toList();

        Map<Long, Item> itemMap = itemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, item -> item));

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderRequest.OrderItemRequest itemReq : request.items()) {
            Item item = itemMap.get(itemReq.itemId());

            if (item == null) {
                throw new EntityNotFoundException("Item not found with id: " + itemReq.itemId());
            }

            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));
            totalPrice = totalPrice.add(itemTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setItem(item);
            orderItem.setQuantity(itemReq.quantity());
            order.addItem(orderItem);
        }

        order.setTotalPrice(totalPrice);
        Order savedOrder = orderRepository.save(order);

        return buildCombinedResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderWithUserResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
        return buildCombinedResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderWithUserResponse> getOrders(LocalDateTime startDate, LocalDateTime endDate, List<String> statuses, Pageable pageable) {
        return orderRepository.findAll(OrderSpecification.filterByDateAndStatus(startDate, endDate, statuses), pageable)
                .map(this::buildCombinedResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderWithUserResponse> getOrdersByUserId(Long userId, Pageable pageable) {
        return orderRepository.findAllByUserId(userId, pageable)
                .map(this::buildCombinedResponse);
    }

    @Transactional
    public OrderWithUserResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
        order.setStatus(newStatus);
        return buildCombinedResponse(orderRepository.save(order));
    }

    @Transactional
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new EntityNotFoundException("Order not found: " + id);
        }
        orderRepository.deleteById(id);
    }


    private OrderWithUserResponse buildCombinedResponse(Order order) {
        OrderResponse orderDto = orderMapper.toDto(order);
        UserDto userDto = userServiceClient.getUserById(order.getUserId());
        return new OrderWithUserResponse(orderDto, userDto);
    }
}