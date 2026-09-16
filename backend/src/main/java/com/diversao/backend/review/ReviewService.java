package com.diversao.backend.review;

import com.diversao.backend.exception.BadRequestException;
import com.diversao.backend.exception.ConflictException;
import com.diversao.backend.exception.ErrorMessages;
import com.diversao.backend.exception.ResourceNotFoundException;
import com.diversao.backend.product.Product;
import com.diversao.backend.product.ProductRepository;
import com.diversao.backend.review.ReviewRequest;
import com.diversao.backend.review.ReviewResponse;
import com.diversao.backend.review.ReviewMapper;
import com.diversao.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final ReviewMapper reviewMapper;

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByProduct(Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviews = reviewRepository.findByProductId(productId, pageable);
        return reviews.map(reviewMapper::toResponse);
    }

    @Transactional
    public ReviewResponse createReview(User user, ReviewRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.PRODUCT_NOT_FOUND + request.productId()));

        if (reviewRepository.existsByProductIdAndUserId(product.getId(), user.getId())) {
            throw new ConflictException(ErrorMessages.USER_ALREADY_REVIEWED);
        }

        Review review = reviewMapper.toEntity(request);
        review.setProduct(product);
        review.setUser(user);
        review = reviewRepository.save(review);
        return reviewMapper.toResponse(review);
    }

    @Transactional
    public void deleteReview(Long reviewId, User user, boolean isAdmin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.REVIEW_NOT_FOUND + reviewId));

        if (!isAdmin && !review.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException(ErrorMessages.CAN_ONLY_DELETE_OWN_REVIEWS);
        }
        reviewRepository.delete(review);
    }
}