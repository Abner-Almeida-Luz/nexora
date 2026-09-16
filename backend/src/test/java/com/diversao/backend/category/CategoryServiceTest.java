package com.diversao.backend.category;

import com.diversao.backend.category.CategoryRequest;
import com.diversao.backend.category.CategoryResponse;
import com.diversao.backend.category.CategoryMapper;
import com.diversao.backend.exception.ConflictException;
import com.diversao.backend.exception.ResourceNotFoundException;
import com.diversao.backend.product.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de CategoryService: CRUD e regras de unicidade de nome.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock CategoryRepository categoryRepository;
    @Mock CategoryMapper categoryMapper;
    @Mock ProductRepository productRepository;

    @InjectMocks CategoryService categoryService;

    @Test
    @DisplayName("findAll: mapeia cada entidade para response")
    void findAll_mapeiaEntidades() {
        var c1 = Category.builder().id(1L).name("Livros").build();
        var c2 = Category.builder().id(2L).name("Jogos").build();
        when(categoryRepository.findAll()).thenReturn(List.of(c1, c2));
        when(categoryMapper.toResponse(c1)).thenReturn(new CategoryResponse(1L, "Livros"));
        when(categoryMapper.toResponse(c2)).thenReturn(new CategoryResponse(2L, "Jogos"));

        var result = categoryService.findAll();

        assertEquals(2, result.size());
        assertEquals("Livros", result.get(0).name());
    }

    @Test
    @DisplayName("findById: devolve response da categoria existente")
    void findById_happyPath() {
        var c = Category.builder().id(1L).name("Livros").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(c));
        when(categoryMapper.toResponse(c)).thenReturn(new CategoryResponse(1L, "Livros"));

        var result = categoryService.findById(1L);

        assertEquals(1L, result.id());
        assertEquals("Livros", result.name());
    }

    @Test
    @DisplayName("findById: id inexistente lança ResourceNotFoundException")
    void findById_naoEncontrado() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> categoryService.findById(99L));
    }

    @Test
    @DisplayName("create: salva categoria com nome inédito")
    void create_happyPath() {
        var req = new CategoryRequest("Livros");
        var entity = Category.builder().name("Livros").build();
        when(categoryRepository.existsByName("Livros")).thenReturn(false);
        when(categoryMapper.toEntity(req)).thenReturn(entity);
        when(categoryRepository.save(entity)).thenAnswer(inv -> {
            entity.setId(1L);
            return entity;
        });
        when(categoryMapper.toResponse(entity)).thenReturn(new CategoryResponse(1L, "Livros"));

        var result = categoryService.create(req);

        assertEquals(1L, result.id());
        verify(categoryRepository).save(entity);
    }

    @Test
    @DisplayName("create: nome duplicado lança ConflictException")
    void create_nomeDuplicado() {
        var req = new CategoryRequest("Livros");
        when(categoryRepository.existsByName("Livros")).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.create(req));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("update: altera nome quando o novo nome é inédito")
    void update_happyPath() {
        var existing = Category.builder().id(1L).name("Livros").build();
        var req = new CategoryRequest("Quadrinhos");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByName("Quadrinhos")).thenReturn(false);
        when(categoryRepository.save(existing)).thenReturn(existing);
        when(categoryMapper.toResponse(existing)).thenReturn(new CategoryResponse(1L, "Quadrinhos"));

        var result = categoryService.update(1L, req);

        assertEquals("Quadrinhos", existing.getName());
        assertEquals("Quadrinhos", result.name());
    }

    @Test
    @DisplayName("update: categoria inexistente lança ResourceNotFoundException")
    void update_categoriaNaoEncontrada() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.update(99L, new CategoryRequest("X")));
    }

    @Test
    @DisplayName("update: nome duplicado (diferente do atual) lança ConflictException")
    void update_nomeDuplicado() {
        var existing = Category.builder().id(1L).name("Livros").build();
        var req = new CategoryRequest("Jogos");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByName("Jogos")).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.update(1L, req));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("update: manter o mesmo nome não dispara ConflictException")
    void update_mesmoNomeNaoDisparaConflito() {
        var existing = Category.builder().id(1L).name("Livros").build();
        var req = new CategoryRequest("Livros");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(existing)).thenReturn(existing);
        when(categoryMapper.toResponse(existing)).thenReturn(new CategoryResponse(1L, "Livros"));

        assertDoesNotThrow(() -> categoryService.update(1L, req));
        verify(categoryRepository, never()).existsByName(any());
    }

    @Test
    @DisplayName("delete: remove categoria existente sem produtos associados")
    void delete_happyPath() {
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.delete(1L);

        verify(categoryRepository).deleteById(1L);
    }

    @Test
    @DisplayName("delete: id inexistente lança ResourceNotFoundException")
    void delete_naoEncontrado() {
        when(categoryRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> categoryService.delete(99L));
        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete: categoria com produtos associados lança ConflictException")
    void delete_categoriaComProdutos() {
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.delete(1L));
        verify(categoryRepository, never()).deleteById(any());
    }
}