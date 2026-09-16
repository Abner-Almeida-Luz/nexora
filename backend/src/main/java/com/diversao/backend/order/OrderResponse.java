package com.diversao.backend.order;

import com.diversao.backend.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        OrderStatus status,
        LocalDateTime createdAt,
        List<OrderItemResponse> items,
        java.math.BigDecimal total
) {}