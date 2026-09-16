package com.ecommerce.product.config;

import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initProductData(ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() == 0) {
                List<Product> initialProducts = List.of(
                        new Product(null, "Apple iPhone 15", "Latest 128GB Midnight Blue flagship smartphone",
                                new BigDecimal("799.99"), 50),
                        new Product(null, "Sony WH-1000XM5", "Wireless Noise Cancelling Headphones, Black",
                                new BigDecimal("349.99"), 30),
                        new Product(null, "Dell XPS 15 Laptop", "15.6 inch OLED, Intel Core i7, 16GB RAM, 512GB SSD",
                                new BigDecimal("1499.99"), 15),
                        new Product(null, "Logitech MX Master 3S", "Ergonomic performance wireless mouse with quiet clicks",
                                new BigDecimal("99.99"), 100)
                );
                productRepository.saveAll(initialProducts);
                log.info("Successfully initialized {} sample products in product-service database.", initialProducts.size());
            }
        };
    }
}
