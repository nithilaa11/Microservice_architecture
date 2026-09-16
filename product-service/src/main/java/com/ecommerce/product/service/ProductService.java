package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductDTO;
import com.ecommerce.product.dto.StockResponseDTO;

import java.util.List;

public interface ProductService {
    ProductDTO createProduct(ProductDTO productDTO);
    List<ProductDTO> getAllProducts();
    ProductDTO getProductById(Long id);
    ProductDTO updateProduct(Long id, ProductDTO productDTO);
    void deleteProduct(Long id);
    StockResponseDTO checkStock(Long productId);
    StockResponseDTO reduceStock(Long productId, Integer quantity);
    StockResponseDTO restoreStock(Long productId, Integer quantity);
}
