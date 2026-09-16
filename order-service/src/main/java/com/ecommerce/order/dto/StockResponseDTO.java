package com.ecommerce.order.dto;

public class StockResponseDTO {
    private Long productId;
    private String productName;
    private Integer availableQuantity;
    private boolean inStock;

    public StockResponseDTO() {
    }

    public StockResponseDTO(Long productId, String productName, Integer availableQuantity, boolean inStock) {
        this.productId = productId;
        this.productName = productName;
        this.availableQuantity = availableQuantity;
        this.inStock = inStock;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public boolean isInStock() {
        return inStock;
    }

    public void setInStock(boolean inStock) {
        this.inStock = inStock;
    }
}
