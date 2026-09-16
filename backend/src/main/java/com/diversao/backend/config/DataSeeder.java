package com.diversao.backend.config;

import com.diversao.backend.category.Category;
import com.diversao.backend.category.CategoryRepository;
import com.diversao.backend.product.Product;
import com.diversao.backend.product.ProductRepository;
import com.diversao.backend.user.Role;
import com.diversao.backend.user.User;
import com.diversao.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @Profile("dev")
    public CommandLineRunner seedData() {
        return args -> {
            // Usuários
            if (userRepository.count() == 0) {
                User admin = User.builder()
                        .name("Admin")
                        .email("admin@diversao.com")
                        .password(passwordEncoder.encode("admin123"))
                        .role(Role.ADMIN)
                        .build();
                User user = User.builder()
                        .name("Cliente")
                        .email("cliente@diversao.com")
                        .password(passwordEncoder.encode("cliente123"))
                        .role(Role.USER)
                        .build();
                userRepository.save(admin);
                userRepository.save(user);
            }

            // Categorias
            Category eletronicos = null;
            if (categoryRepository.count() == 0) {
                eletronicos = Category.builder().name("Eletrônicos").build();
                Category livros = Category.builder().name("Livros").build();
                Category jogos = Category.builder().name("Jogos").build();
                categoryRepository.save(eletronicos);
                categoryRepository.save(livros);
                categoryRepository.save(jogos);
            } else {
                eletronicos = categoryRepository.findByName("Eletrônicos").orElseThrow();
            }

            // Produtos
            if (productRepository.count() == 0) {
                Product produto1 = Product.builder()
                        .name("Smartphone XYZ")
                        .description("Um smartphone moderno com câmera de alta resolução")
                        .price(new BigDecimal("1999.90"))
                        .stock(50)
                        .imageUrl("https://example.com/smartphone.jpg")
                        .category(eletronicos)
                        .build();
                Product produto2 = Product.builder()
                        .name("Notebook ABC")
                        .description("Notebook leve e potente para trabalho e estudo")
                        .price(new BigDecimal("3499.90"))
                        .stock(30)
                        .imageUrl("https://example.com/notebook.jpg")
                        .category(eletronicos)
                        .build();
                productRepository.save(produto1);
                productRepository.save(produto2);
            }
        };
    }
}