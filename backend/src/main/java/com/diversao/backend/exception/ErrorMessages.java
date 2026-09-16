package com.diversao.backend.exception;

/**
 * Mensagens de erro de domínio. Centralizadas para evitar strings mágicas
 * duplicadas entre Services e testes. Não é um arquivo de i18n — é só uma
 * fonte única de verdade para os textos que aparecem em ProblemDetail.
 */
public final class ErrorMessages {

    // Product
    public static final String PRODUCT_NOT_FOUND = "Product not found with id: ";
    public static final String INSUFFICIENT_STOCK = "Insufficient stock for product: ";

    // Category
    public static final String CATEGORY_NOT_FOUND = "Category not found with id: ";
    public static final String CATEGORY_NAME_ALREADY_EXISTS = "Category name already exists";
    public static final String CATEGORY_HAS_PRODUCTS = "Category has products associated and cannot be deleted";

    // User
    public static final String USER_NOT_FOUND = "User not found";
    public static final String EMAIL_ALREADY_IN_USE = "Email already in use";
    public static final String INVALID_CREDENTIALS = "Invalid credentials";

    // Cart
    public static final String CART_EMPTY = "Cart is empty";
    public static final String CART_ITEM_NOT_FOUND = "Item not found in cart for product id: ";
    public static final String TOTAL_QUANTITY_EXCEEDS_STOCK = "Total quantity exceeds available stock";
    public static final String QUANTITY_EXCEEDS_STOCK = "Quantity exceeds available stock";

    // Order
    public static final String ORDER_NOT_FOUND = "Order not found with id: ";
    public static final String CAN_ONLY_ACCESS_OWN_ORDERS = "You can only access your own orders";
    public static final String ONLY_PENDING_ORDERS_CAN_BE_CANCELLED = "Only pending orders can be cancelled";

    // Review
    public static final String REVIEW_NOT_FOUND = "Review not found with id: ";
    public static final String USER_ALREADY_REVIEWED = "User has already reviewed this product";
    public static final String CAN_ONLY_DELETE_OWN_REVIEWS = "You can only delete your own reviews";

    // Auth
    public static final String TOO_MANY_LOGIN_ATTEMPTS = "Too many login attempts, try again later";

    private ErrorMessages() {}
}