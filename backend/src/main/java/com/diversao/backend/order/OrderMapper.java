package com.diversao.backend.order;

import com.diversao.backend.order.Order;
import com.diversao.backend.order.OrderItem;
import com.diversao.backend.order.OrderItemResponse;
import com.diversao.backend.order.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "subtotal", expression = "java(orderItem.getPrice().multiply(java.math.BigDecimal.valueOf(orderItem.getQuantity())))")
    OrderItemResponse toItemResponse(OrderItem orderItem);

    @Mapping(target = "userId", source = "user.id")
    OrderResponse toResponse(Order order);
}