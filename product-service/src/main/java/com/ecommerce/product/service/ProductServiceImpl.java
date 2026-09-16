package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductDTO;
import com.ecommerce.product.dto.StockResponseDTO;
import com.ecommerce.product.exception.InsufficientStockException;
import com.ecommerce.product.exception.ResourceNotFoundException;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);
    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public ProductDTO createProduct(ProductDTO productDTO) {
        Product product = new Product();
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setQuantity(productDTO.getQuantity());

        Product saved = productRepository.save(product);
        log.info("Created product ID {} with name '{}'", saved.getId(), saved.getName());
        return mapToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToDTO(product);
    }

    @Override
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setQuantity(productDTO.getQuantity());

        Product updated = productRepository.save(product);
        log.info("Updated product ID {}", updated.getId());
        return mapToDTO(updated);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        productRepository.delete(product);
        log.info("Deleted product ID {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public StockResponseDTO checkStock(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        boolean inStock = product.getQuantity() > 0;
        return new StockResponseDTO(product.getId(), product.getName(), product.getQuantity(), inStock);
    }

    @Override
    public StockResponseDTO reduceStock(Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity to reduce must be greater than zero");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (product.getQuantity() < quantity) {
            log.warn("Insufficient stock for product ID {}. Available: {}, Requested: {}",
                    productId, product.getQuantity(), quantity);
            throw new InsufficientStockException("Insufficient stock for product '" + product.getName() +
                    "'. Available: " + product.getQuantity() + ", Requested: " + quantity);
        }

        product.setQuantity(product.getQuantity() - quantity);
        Product updated = productRepository.save(product);

        log.info("Reduced stock for product ID {} by {}. Remaining stock: {}", productId, quantity, updated.getQuantity());
        return new StockResponseDTO(updated.getId(), updated.getName(), updated.getQuantity(), updated.getQuantity() > 0);
    }

    @Override
    public StockResponseDTO restoreStock(Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity to restore must be greater than zero");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        product.setQuantity(product.getQuantity() + quantity);
        Product updated = productRepository.save(product);

        log.info("Restored stock for product ID {} by {}. New stock: {}", productId, quantity, updated.getQuantity());
        return new StockResponseDTO(updated.getId(), updated.getName(), updated.getQuantity(), updated.getQuantity() > 0);
    }

    private ProductDTO mapToDTO(Product product) {
        return new ProductDTO(product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), product.getQuantity());
    }
}
