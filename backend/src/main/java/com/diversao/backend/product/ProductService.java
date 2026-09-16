package com.diversao.backend.product;

import com.diversao.backend.category.Category;
import com.diversao.backend.category.CategoryRepository;
import com.diversao.backend.exception.ErrorMessages;
import com.diversao.backend.exception.ResourceNotFoundException;
import com.diversao.backend.security.AuditLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final AuditLogger auditLogger;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "products", key = "'list:' + #search + ':' + #categoryId + ':' + #page + ':' + #size")
    public Page<ProductResponse> findAll(String search, Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Product> products = productRepository.searchProducts(search, categoryId, pageable);
        return products.map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "products", key = "'id:' + #id")
    public ProductResponse findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.PRODUCT_NOT_FOUND + id));
        return productMapper.toResponse(product);
    }

    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductResponse create(ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.CATEGORY_NOT_FOUND + request.categoryId()));

        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        product = productRepository.save(product);
        auditLogger.log("PRODUCT_CREATE", category.getId());
        return productMapper.toResponse(product);
    }

    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.PRODUCT_NOT_FOUND + id));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.CATEGORY_NOT_FOUND + request.categoryId()));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setImageUrl(request.imageUrl());
        product.setCategory(category);

        product = productRepository.save(product);
        auditLogger.log("PRODUCT_UPDATE", category.getId());
        return productMapper.toResponse(product);
    }

    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true)
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException(ErrorMessages.PRODUCT_NOT_FOUND + id);
        }
        productRepository.deleteById(id);
        auditLogger.log("PRODUCT_DELETE", id);
    }
}