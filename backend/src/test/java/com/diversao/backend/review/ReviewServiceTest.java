package com.diversao.backend.review;

import com.diversao.backend.exception.BadRequestException;
import com.diversao.backend.exception.ConflictException;
import com.diversao.backend.exception.ResourceNotFoundException;
import com.diversao.backend.product.Product;
import com.diversao.backend.product.ProductRepository;
import com.diversao.backend.review.ReviewRequest;
import com.diversao.backend.review.ReviewResponse;
import com.diversao.backend.review.ReviewMapper;
import com.diversao.backend.user.Role;
import com.diversao.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de ReviewService: listagem paginada, criação com regra
 * única (product, user) e remoção com controle de autoria/role.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock ProductRepository productRepository;
    @Mock ReviewMapper reviewMapper;

    @InjectMocks ReviewService reviewService;

    private User author;
    private User otherUser;
    private Product product;

    @BeforeEach
    void setup() {
        author = User.builder().id(1L).name("Ana").role(Role.USER).build();
        otherUser = User.builder().id(2L).name("Bia").role(Role.USER).build();
        product = Product.builder().id(100L).name("Notebook").build();
    }

    @Test
    @DisplayName("getReviewsByProduct: devolve página mapeada")
    void getReviewsByProduct_happyPath() {
        var r = Review.builder().id(1L).product(product).user(author).rating(5).build();
        var page = new PageImpl<>(List.of(r));
        when(reviewRepository.findByProductId(eq(100L), any(Pageable.class))).thenReturn(page);
        when(reviewMapper.toResponse(r)).thenReturn(
                new ReviewResponse(1L, 100L, 1L, "Ana", 5, "bom", null));

        var result = reviewService.getReviewsByProduct(100L, 0, 10);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("createReview: salva review quando o usuário ainda não avaliou o produto")
    void createReview_happyPath() {
        var req = new ReviewRequest(100L, 5, "top");
        var entity = Review.builder().build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByProductIdAndUserId(100L, 1L)).thenReturn(false);
        when(reviewMapper.toEntity(req)).thenReturn(entity);
        when(reviewRepository.save(entity)).thenAnswer(inv -> {
            entity.setId(1L);
            return entity;
        });
        when(reviewMapper.toResponse(entity)).thenReturn(
                new ReviewResponse(1L, 100L, 1L, "Ana", 5, "top", null));

        var result = reviewService.createReview(author, req);

        assertEquals(1L, result.id());
        assertSame(product, entity.getProduct());
        assertSame(author, entity.getUser());
    }

    @Test
    @DisplayName("createReview: produto inexistente lança ResourceNotFoundException")
    void createReview_produtoNaoEncontrado() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.createReview(author, new ReviewRequest(999L, 5, null)));
    }

    @Test
    @DisplayName("createReview: usuário já avaliou o produto lança ConflictException")
    void createReview_duplicado() {
        var req = new ReviewRequest(100L, 5, null);
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByProductIdAndUserId(100L, 1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> reviewService.createReview(author, req));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteReview: autor apaga a própria review")
    void deleteReview_autor() {
        var review = Review.builder().id(1L).user(author).build();
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        reviewService.deleteReview(1L, author, false);

        verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("deleteReview: ADMIN apaga review de outro usuário")
    void deleteReview_admin() {
        var review = Review.builder().id(1L).user(otherUser).build();
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        reviewService.deleteReview(1L, author, true);

        verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("deleteReview: usuário comum tentando apagar review alheia lança BadRequestException")
    void deleteReview_naoAutorNaoAdmin() {
        var review = Review.builder().id(1L).user(otherUser).build();
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> reviewService.getReviewsByProduct(1L, 0, 0));
        verify(reviewRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteReview: id inexistente lança ResourceNotFoundException")
    void deleteReview_naoEncontrado() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.deleteReview(99L, author, true));
    }
}