package com.diversao.backend.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByProductId(Long productId, Pageable pageable);
    boolean existsByProductIdAndUserId(Long productId, Long userId);
    boolean existsByIdAndUserId(Long reviewId, Long userId);
}