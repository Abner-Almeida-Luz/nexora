package com.diversao.backend.order;

import com.diversao.backend.cart.Cart;
import com.diversao.backend.cart.CartItem;
import com.diversao.backend.cart.CartItemRepository;
import com.diversao.backend.cart.CartRepository;
import com.diversao.backend.exception.BadRequestException;
import com.diversao.backend.exception.ResourceNotFoundException;
import com.diversao.backend.order.OrderResponse;
import com.diversao.backend.order.UpdateOrderStatusRequest;
import com.diversao.backend.order.OrderMapper;
import com.diversao.backend.product.Product;
import com.diversao.backend.product.ProductRepository;
import com.diversao.backend.user.Role;
import com.diversao.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de OrderService: criação de pedido (baixa de estoque,
 * congelamento de preço, limpeza do carrinho), consulta e mudança de status.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock ProductRepository productRepository;
    @Mock OrderMapper orderMapper;

    @InjectMocks OrderService orderService;

    private User user;
    private Product product;
    private Cart cart;

    @BeforeEach
    void setup() {
        user = User.builder().id(1L).email("u@x.com").role(Role.USER).build();
        product = Product.builder().id(100L).name("Notebook")
                .price(new BigDecimal("10.00")).stock(5).build();
        cart = Cart.builder().id(10L).user(user).items(new ArrayList<>()).build();
    }

    private void addItemToCart(int qty) {
        var item = CartItem.builder().id(50L).cart(cart).product(product).quantity(qty).build();
        cart.getItems().add(item);
    }

    @Test
    @DisplayName("createOrder: carrinho inexistente lança BadRequestException")
    void createOrder_semCarrinho() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> orderService.createOrder(user));
    }

    @Test
    @DisplayName("createOrder: carrinho vazio lança BadRequestException")
    void createOrder_carrinhoVazio() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        assertThrows(BadRequestException.class, () -> orderService.createOrder(user));
    }

    @Test
    @DisplayName("createOrder: estoque insuficiente lança BadRequestException")
    void createOrder_estoqueInsuficiente() {
        addItemToCart(6); // stock=5
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThrows(BadRequestException.class, () -> orderService.createOrder(user));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOrder: decrementa estoque, congela preço e limpa o carrinho")
    void createOrder_happyPath() {
        addItemToCart(2);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(500L);
            return o;
        });
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toResponse(any(Order.class)))
                .thenReturn(new OrderResponse(500L, 1L, OrderStatus.PENDING,
                        null, List.of(), new BigDecimal("20.00")));

        var response = orderService.createOrder(user);

        // Estoque decrementado: 5 - 2 = 3
        assertEquals(3, product.getStock());

        // Captura o que foi salvo como Order para inspecionar itens e preço congelado
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();

        assertEquals(new BigDecimal("20.00"), saved.getTotal());
        assertEquals(1, saved.getItems().size());
        assertEquals(new BigDecimal("10.00"), saved.getItems().get(0).getPrice());
        assertEquals("Notebook", saved.getItems().get(0).getProductName());
        assertEquals(2, saved.getItems().get(0).getQuantity());

        // Carrinho limpo
        verify(cartItemRepository).deleteByCartId(10L);
        assertTrue(cart.getItems().isEmpty());

        assertEquals(500L, response.id());
    }

    @Test
    @DisplayName("createOrder: preço no OrderItem não muda se o Product mudar depois")
    void createOrder_precoCongelado() {
        addItemToCart(1);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toResponse(any(Order.class)))
                .thenReturn(new OrderResponse(1L, 1L, OrderStatus.PENDING, null, List.of(), BigDecimal.TEN));

        orderService.createOrder(user);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        var item = captor.getValue().getItems().get(0);

        // Altera o preço do produto "depois"
        product.setPrice(new BigDecimal("999.99"));

        // O OrderItem preserva o preço do momento da compra
        assertEquals(new BigDecimal("10.00"), item.getPrice());
    }

    @Test
    @DisplayName("getUserOrders: mapeia os pedidos do usuário")
    void getUserOrders_happyPath() {
        var o = Order.builder().id(1L).user(user).build();
        when(orderRepository.findByUserId(1L)).thenReturn(List.of(o));
        when(orderMapper.toResponse(o)).thenReturn(
                new OrderResponse(1L, 1L, OrderStatus.PENDING, null, List.of(), BigDecimal.TEN));

        var result = orderService.getUserOrders(user);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getOrderById: dono do pedido consegue ver")
    void getOrderById_dono() {
        var o = Order.builder().id(1L).user(user).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
        when(orderMapper.toResponse(o)).thenReturn(
                new OrderResponse(1L, 1L, OrderStatus.PENDING, null, List.of(), BigDecimal.TEN));

        assertNotNull(orderService.getOrderById(1L, user, false));
    }

    @Test
    @DisplayName("getOrderById: ADMIN vê pedido de outro usuário")
    void getOrderById_admin() {
        var other = User.builder().id(99L).build();
        var o = Order.builder().id(1L).user(other).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
        when(orderMapper.toResponse(o)).thenReturn(
                new OrderResponse(1L, 99L, OrderStatus.PENDING, null, List.of(), BigDecimal.TEN));

        assertNotNull(orderService.getOrderById(1L, user, true));
    }

    @Test
    @DisplayName("getOrderById: usuário comum não vê pedido alheio → BadRequestException (ver ambiguidade #3)")
    void getOrderById_naoDono() {
        var other = User.builder().id(99L).build();
        var o = Order.builder().id(1L).user(other).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> orderService.getOrderById(1L, user, false));
    }

    @Test
    @DisplayName("getOrderById: id inexistente lança ResourceNotFoundException")
    void getOrderById_naoEncontrado() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> orderService.getOrderById(99L, user, false));
    }

    @Test
    @DisplayName("getAllOrders: devolve lista completa")
    void getAllOrders_happyPath() {
        var o1 = Order.builder().id(1L).user(user).build();
        when(orderRepository.findAll()).thenReturn(List.of(o1));
        when(orderMapper.toResponse(o1)).thenReturn(
                new OrderResponse(1L, 1L, OrderStatus.PENDING, null, List.of(), BigDecimal.TEN));

        assertEquals(1, orderService.getAllOrders().size());
    }

    @Test
    @DisplayName("updateOrderStatus: altera o status do pedido")
    void updateOrderStatus_happyPath() {
        var o = Order.builder().id(1L).user(user).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
        when(orderRepository.save(o)).thenReturn(o);
        when(orderMapper.toResponse(o)).thenReturn(
                new OrderResponse(1L, 1L, OrderStatus.SHIPPED, null, List.of(), BigDecimal.TEN));

        orderService.updateOrderStatus(1L, new UpdateOrderStatusRequest(OrderStatus.SHIPPED));

        assertEquals(OrderStatus.SHIPPED, o.getStatus());
    }

    @Test
    @DisplayName("updateOrderStatus: id inexistente lança ResourceNotFoundException")
    void updateOrderStatus_naoEncontrado() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> orderService.updateOrderStatus(99L,
                        new UpdateOrderStatusRequest(OrderStatus.SHIPPED)));
    }
}