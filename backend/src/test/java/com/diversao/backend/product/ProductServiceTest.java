package com.diversao.backend.product;

import com.diversao.backend.category.Category;
import com.diversao.backend.category.CategoryRepository;
import com.diversao.backend.exception.ResourceNotFoundException;
import com.diversao.backend.product.ProductRequest;
import com.diversao.backend.product.ProductResponse;
import com.diversao.backend.product.ProductMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de ProductService: listagem paginada, busca por filtro e CRUD.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock ProductMapper productMapper;

    @InjectMocks ProductService productService;

    private ProductRequest sampleRequest(Long categoryId) {
        return new ProductRequest("Notebook", "desc",
                new BigDecimal("1999.90"), 10, null, categoryId);
    }

    @Test
    @DisplayName("findAll: mapeia cada produto da página para response")
    void findAll_happyPath() {
        var p = Product.builder().id(1L).name("Notebook").build();
        var page = new PageImpl<>(List.of(p));
        when(productRepository.searchProducts(eq(null), eq(null), any(Pageable.class))).thenReturn(page);
        when(productMapper.toResponse(p)).thenReturn(
                new ProductResponse(1L, "Notebook", "d", BigDecimal.TEN, 1, null, 1L, "Cat"));

        var result = productService.findAll(null, null, 0, 10);

        assertEquals(1, result.getTotalElements());
        verify(productRepository).searchProducts(isNull(), isNull(),
                argThat(pg -> pg.getPageNumber() == 0 && pg.getPageSize() == 10));
    }

    @Test
    @DisplayName("findAll: categoryId inexistente retorna página vazia (sem exceção)")
    void findAll_categoryIdInexistente_retornaVazio() {
        when(productRepository.searchProducts(any(), eq(999L), any(Pageable.class)))
                .thenReturn(Page.empty());

        var result = productService.findAll(null, 999L, 0, 10);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findById: devolve response do produto existente")
    void findById_happyPath() {
        var p = Product.builder().id(1L).name("Notebook").build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(productMapper.toResponse(p)).thenReturn(
                new ProductResponse(1L, "Notebook", "d", BigDecimal.TEN, 1, null, 1L, "Cat"));

        var result = productService.findById(1L);

        assertEquals("Notebook", result.name());
    }

    @Test
    @DisplayName("findById: id inexistente lança ResourceNotFoundException")
    void findById_naoEncontrado() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> productService.findById(99L));
    }

    @Test
    @DisplayName("create: salva produto com a categoria correta")
    void create_happyPath() {
        var req = sampleRequest(1L);
        var cat = Category.builder().id(1L).name("Eletro").build();
        var entity = Product.builder().name("Notebook").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(productMapper.toEntity(req)).thenReturn(entity);
        when(productRepository.save(entity)).thenAnswer(inv -> {
            entity.setId(1L);
            return entity;
        });
        when(productMapper.toResponse(entity)).thenReturn(
                new ProductResponse(1L, "Notebook", "d", BigDecimal.TEN, 10, null, 1L, "Eletro"));

        var result = productService.create(req);

        assertEquals("Notebook", result.name());
        assertSame(cat, entity.getCategory());
    }

    @Test
    @DisplayName("create: categoria inexistente lança ResourceNotFoundException")
    void create_categoriaNaoEncontrada() {
        var req = sampleRequest(99L);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.create(req));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("update: substitui campos e categoria")
    void update_happyPath() {
        var existing = Product.builder().id(1L).name("Old").build();
        var cat = Category.builder().id(1L).name("Eletro").build();
        var req = sampleRequest(1L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(productRepository.save(existing)).thenReturn(existing);
        when(productMapper.toResponse(existing)).thenReturn(
                new ProductResponse(1L, "Notebook", "desc", new BigDecimal("1999.90"), 10, null, 1L, "Eletro"));

        var result = productService.update(1L, req);

        assertEquals("Notebook", existing.getName());
        assertEquals(10, existing.getStock());
        assertSame(cat, existing.getCategory());
        assertEquals("Notebook", result.name());
    }

    @Test
    @DisplayName("update: produto inexistente lança ResourceNotFoundException")
    void update_produtoNaoEncontrado() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> productService.update(99L, sampleRequest(1L)));
    }

    @Test
    @DisplayName("update: categoria inexistente lança ResourceNotFoundException")
    void update_categoriaNaoEncontrada() {
        var existing = Product.builder().id(1L).name("Old").build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.update(1L, sampleRequest(99L)));
    }

    @Test
    @DisplayName("delete: remove produto existente")
    void delete_happyPath() {
        when(productRepository.existsById(1L)).thenReturn(true);
        productService.delete(1L);
        verify(productRepository).deleteById(1L);
    }

    @Test
    @DisplayName("delete: id inexistente lança ResourceNotFoundException")
    void delete_naoEncontrado() {
        when(productRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> productService.delete(99L));
        verify(productRepository, never()).deleteById(any());
    }
}