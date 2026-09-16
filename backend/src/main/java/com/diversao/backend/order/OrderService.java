package com.diversao.backend.order;

import com.diversao.backend.cart.Cart;
import com.diversao.backend.cart.CartItem;
import com.diversao.backend.cart.CartRepository;
import com.diversao.backend.cart.CartItemRepository;
import com.diversao.backend.exception.BadRequestException;
import com.diversao.backend.exception.ErrorMessages;
import com.diversao.backend.exception.ResourceNotFoundException;
import com.diversao.backend.order.OrderResponse;
import com.diversao.backend.order.UpdateOrderStatusRequest;
import com.diversao.backend.order.OrderMapper;
import com.diversao.backend.product.Product;
import com.diversao.backend.product.ProductRepository;
import com.diversao.backend.security.AuditLogger;
import com.diversao.backend.user.User;
import com.diversao.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final AuditLogger auditLogger;

    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true)
    public OrderResponse createOrder(User user) {
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException(ErrorMessages.CART_EMPTY));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException(ErrorMessages.CART_EMPTY);
        }

        // Verificar estoque e calcular total
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            // Bloqueio pessimista opcional: poderíamos usar lock no banco.
            // Para MVP, confiamos na transação e em verificações.
            if (product.getStock() < item.getQuantity()) {
                throw new BadRequestException(ErrorMessages.INSUFFICIENT_STOCK + product.getName());
            }
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // Criar Order
        Order order = Order.builder()
                .user(user)
                .total(total)
                .build();

        // Criar OrderItems e decrementar estoque
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .price(product.getPrice()) // congelando o preço
                    .quantity(cartItem.getQuantity())
                    .build();
            order.getItems().add(orderItem);
        }

        order = orderRepository.save(order);

        // Limpar carrinho
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        cartRepository.save(cart);

        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(User user) {
        return orderRepository.findByUserId(user.getId()).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id, User user, boolean isAdmin) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.ORDER_NOT_FOUND + id));

        if (!isAdmin && !order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException(ErrorMessages.CAN_ONLY_ACCESS_OWN_ORDERS);
        }
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.ORDER_NOT_FOUND + id));
        order.setStatus(request.status());
        order = orderRepository.save(order);
        auditLogger.log("ORDER_STATUS_CHANGE", order.getId());
        return orderMapper.toResponse(order);
    }

    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true)
    public OrderResponse cancelOrder(Long id, User user) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.ORDER_NOT_FOUND + id));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException(ErrorMessages.CAN_ONLY_ACCESS_OWN_ORDERS);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException(ErrorMessages.ONLY_PENDING_ORDERS_CAN_BE_CANCELLED);
        }

        // Devolve estoque de cada item
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        return orderMapper.toResponse(order);
    }
}