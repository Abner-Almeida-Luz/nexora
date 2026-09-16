package com.diversao.backend.cart;

import com.diversao.backend.cart.CartItemRequest;
import com.diversao.backend.cart.CartItemResponse;
import com.diversao.backend.cart.CartResponse;
import com.diversao.backend.cart.CartMapper;
import com.diversao.backend.exception.BadRequestException;
import com.diversao.backend.exception.ResourceNotFoundException;
import com.diversao.backend.product.Product;
import com.diversao.backend.product.ProductRepository;
import com.diversao.backend.user.Role;
import com.diversao.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * Testes unitários de CartService: adição/atualização de itens, regras de estoque
 * e esvaziamento. Cobre os dois pontos de checagem de estoque do service.
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock ProductRepository productRepository;
    @Mock CartMapper cartMapper;

    @InjectMocks CartService cartService;

    private User user;
    private Cart cart;
    private Product product;

    @BeforeEach
    void setup() {
        user = User.builder().id(1L).email("u@x.com").role(Role.USER).build();
        cart = Cart.builder().id(10L).user(user).items(new ArrayList<>()).build();
        product = Product.builder().id(100L).name("Notebook")
                .price(new BigDecimal("10.00")).stock(5).build();
    }

    @Test
    @DisplayName("getCart: devolve o carrinho do usuário")
    void getCart_happyPath() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        var expected = new CartResponse(10L, List.of(), BigDecimal.ZERO);
        when(cartMapper.toResponse(cart)).thenReturn(expected);

        var result = cartService.getCart(user);

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("getCart: cria carrinho novo se o usuário não tiver um")
    void getCart_criaSeNaoExistir() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> {
            Cart c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });
        when(cartMapper.toResponse(any(Cart.class)))
                .thenReturn(new CartResponse(99L, List.of(), BigDecimal.ZERO));

        var result = cartService.getCart(user);

        assertEquals(99L, result.id());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("addItem: adiciona novo item quando há estoque")
    void addItem_happyPath() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(10L, 100L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartMapper.toResponse(cart)).thenReturn(
                new CartResponse(10L,
                        List.of(new CartItemResponse(100L, "Notebook", 2, BigDecimal.TEN, new BigDecimal("20.00"))),
                        new BigDecimal("20.00")));

        var result = cartService.addItem(user, new CartItemRequest(100L, 2));

        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("20.00"), result.total());
    }

    @Test
    @DisplayName("addItem: produto inexistente lança ResourceNotFoundException")
    void addItem_produtoNaoEncontrado() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cartService.addItem(user, new CartItemRequest(999L, 1)));
    }

    @Test
    @DisplayName("addItem: quantidade pedida > estoque lança BadRequestException")
    void addItem_quantidadeMaiorQueEstoque() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product)); // stock=5

        assertThrows(BadRequestException.class,
                () -> cartService.addItem(user, new CartItemRequest(100L, 6)));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("addItem: soma com item existente e estoura o estoque lança BadRequestException")
    void addItem_somaExcedeEstoque() {
        var existing = CartItem.builder().id(50L).cart(cart).product(product).quantity(3).build();
        cart.getItems().add(existing);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(10L, 100L))
                .thenReturn(Optional.of(existing));

        // 3 + 4 = 7 > 5
        assertThrows(BadRequestException.class,
                () -> cartService.addItem(user, new CartItemRequest(100L, 4)));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateItemQuantity: atualiza quantidade quando dentro do estoque")
    void updateItemQuantity_happyPath() {
        var item = CartItem.builder().id(50L).cart(cart).product(product).quantity(1).build();
        cart.getItems().add(item);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(10L, 100L)).thenReturn(Optional.of(item));
        when(cartItemRepository.save(item)).thenReturn(item);
        when(cartMapper.toResponse(cart)).thenReturn(
                new CartResponse(10L, List.of(), BigDecimal.ZERO));

        cartService.updateItemQuantity(user, 100L, 3);

        assertEquals(3, item.getQuantity());
        verify(cartItemRepository).save(item);
    }

    @Test
    @DisplayName("updateItemQuantity: quantity <= 0 remove o item")
    void updateItemQuantity_removeItemComZero() {
        var item = CartItem.builder().id(50L).cart(cart).product(product).quantity(2).build();
        cart.getItems().add(item);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(10L, 100L)).thenReturn(Optional.of(item));
        when(cartMapper.toResponse(cart)).thenReturn(
                new CartResponse(10L, List.of(), BigDecimal.ZERO));

        cartService.updateItemQuantity(user, 100L, 0);

        assertTrue(cart.getItems().isEmpty());
        verify(cartItemRepository).delete(item);
    }

    @Test
    @DisplayName("updateItemQuantity: quantidade acima do estoque lança BadRequestException")
    void updateItemQuantity_acimaDoEstoque() {
        var item = CartItem.builder().id(50L).cart(cart).product(product).quantity(1).build();
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(10L, 100L)).thenReturn(Optional.of(item));

        assertThrows(BadRequestException.class,
                () -> cartService.updateItemQuantity(user, 100L, 10));
    }

    @Test
    @DisplayName("updateItemQuantity: item inexistente lança ResourceNotFoundException")
    void updateItemQuantity_itemNaoEncontrado() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(10L, 100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cartService.updateItemQuantity(user, 100L, 1));
    }

    @Test
    @DisplayName("clearCart: apaga itens do carrinho")
    void clearCart_happyPath() {
        var item = CartItem.builder().id(50L).cart(cart).product(product).quantity(1).build();
        cart.getItems().add(item);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        cartService.clearCart(user);

        verify(cartItemRepository).deleteByCartId(10L);
        assertTrue(cart.getItems().isEmpty());
    }
}