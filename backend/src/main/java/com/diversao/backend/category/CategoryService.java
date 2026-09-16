package com.diversao.backend.category;

import com.diversao.backend.category.CategoryRequest;
import com.diversao.backend.category.CategoryResponse;
import com.diversao.backend.category.CategoryMapper;
import com.diversao.backend.exception.ConflictException;
import com.diversao.backend.exception.ErrorMessages;
import com.diversao.backend.exception.ResourceNotFoundException;
import com.diversao.backend.security.AuditLogger;
import com.diversao.backend.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.diversao.backend.product.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final ProductRepository productRepository;
    private final AuditLogger auditLogger;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categories", key = "'list'")
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categories", key = "'id:' + #id")
    public CategoryResponse findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.CATEGORY_NOT_FOUND + id));
        return categoryMapper.toResponse(category);
    }

    @Transactional
    @CacheEvict(cacheNames = "categories", allEntries = true)
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new ConflictException(ErrorMessages.CATEGORY_NAME_ALREADY_EXISTS);
        }
        Category category = categoryMapper.toEntity(request);
        category = categoryRepository.save(category);
        auditLogger.log("CATEGORY_CREATE", category.getId());
        return categoryMapper.toResponse(category);
    }

    @Transactional
    @CacheEvict(cacheNames = "categories", allEntries = true)
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.CATEGORY_NOT_FOUND + id));

        if (!category.getName().equals(request.name()) && categoryRepository.existsByName(request.name())) {
            throw new ConflictException(ErrorMessages.CATEGORY_NAME_ALREADY_EXISTS);
        }
        category.setName(request.name());
        category = categoryRepository.save(category);
        auditLogger.log("CATEGORY_UPDATE", category.getId());
        return categoryMapper.toResponse(category);
    }

    @Transactional
    @CacheEvict(cacheNames = "categories", allEntries = true)
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException(ErrorMessages.CATEGORY_NOT_FOUND + id);
        }
        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException(ErrorMessages.CATEGORY_HAS_PRODUCTS);
        }
        categoryRepository.deleteById(id);
        auditLogger.log("CATEGORY_DELETE", id);
    }
}