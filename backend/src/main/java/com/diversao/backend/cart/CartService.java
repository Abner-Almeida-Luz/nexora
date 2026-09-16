package com.diversao.backend.cart;

import com.diversao.backend.cart.CartItemRequest;
import com.diversao.backend.cart.CartResponse;
import com.diversao.backend.cart.CartMapper;
import com.diversao.backend.exception.BadRequestException;
import com.diversao.backend.exception.ErrorMessages;
import com.diversao.backend.exception.ResourceNotFoundException;
import com.diversao.backend.product.Product;
import com.diversao.backend.product.ProductRepository;
import com.diversao.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CartMapper cartMapper;

    @Transactional(readOnly = true)
    public CartResponse getCart(User user) {
        Cart cart = getOrCreateCart(user);
        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(User user, CartItemRequest request) {
        Cart cart = getOrCreateCart(user);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.PRODUCT_NOT_FOUND + request.productId()));

        if (product.getStock() < request.quantity()) {
            throw new BadRequestException(ErrorMessages.INSUFFICIENT_STOCK + product.getName());
        }

        CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        int newQuantity = request.quantity();
        if (existingItem != null) {
            newQuantity = existingItem.getQuantity() + request.quantity();
        }

        if (newQuantity > product.getStock()) {
            throw new BadRequestException(ErrorMessages.TOTAL_QUANTITY_EXCEEDS_STOCK);
        }

        if (existingItem != null) {
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.quantity())
                    .build();
            cart.getItems().add(item);
            cartItemRepository.save(item);
        }

        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(User user, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(user);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.CART_ITEM_NOT_FOUND + productId));

        if (quantity <= 0) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            Product product = item.getProduct();
            if (quantity > product.getStock()) {
                throw new BadRequestException(ErrorMessages.QUANTITY_EXCEEDS_STOCK);
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return cartMapper.toResponse(cart);
    }

    @Transactional
    public void clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart cart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(cart);
                });
    }

    public Cart getCartEntity(User user) {
        return getOrCreateCart(user);
    }
}