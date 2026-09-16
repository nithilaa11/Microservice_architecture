package com.ecommerce.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OrderRequestDTO {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Order quantity must be at least 1")
    private Integer quantity;

    @NotBlank(message = "Payment method is required (e.g., CREDIT_CARD, DEBIT_CARD, UPI)")
    private String paymentMethod;

    // Optional: for testing/demonstrating payment failure compensation
    private Boolean simulatePaymentFailure = false;

    public OrderRequestDTO() {
    }

    public OrderRequestDTO(Long userId, Long productId, Integer quantity, String paymentMethod) {
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.paymentMethod = paymentMethod;
        this.simulatePaymentFailure = false;
    }

    public OrderRequestDTO(Long userId, Long productId, Integer quantity, String paymentMethod, Boolean simulatePaymentFailure) {
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.paymentMethod = paymentMethod;
        this.simulatePaymentFailure = simulatePaymentFailure;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Boolean getSimulatePaymentFailure() {
        return simulatePaymentFailure;
    }

    public void setSimulatePaymentFailure(Boolean simulatePaymentFailure) {
        this.simulatePaymentFailure = simulatePaymentFailure;
    }
}
