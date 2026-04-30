package com.innowise.order_service.kafka;

import com.innowise.order_service.enums.OrderStatus;
import com.innowise.order_service.event.PaymentEvent;
import com.innowise.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "payment-events", groupId = "order-service-group")
    public void consumePaymentEvent(PaymentEvent event) {
        log.info("Received Payment Event from Kafka: {}", event);

        OrderStatus newStatus;
        if ("SUCCESS".equals(event.getPaymentStatus())) {
            newStatus = OrderStatus.PAID;
        } else {
            newStatus = OrderStatus.CANCELLED;
        }

        try {
            orderService.updateOrderStatus(event.getOrderId(), newStatus);
            log.info("Successfully updated Order ID {} to status {}", event.getOrderId(), newStatus);
        } catch (Exception e) {
            log.error("Failed to update Order ID {}: {}", event.getOrderId(), e.getMessage());
        }
    }
}