package com.ecommerce.order.service;

import com.ecommerce.order.client.PaymentClient;
import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.client.UserClient;
import com.ecommerce.order.dto.*;
import com.ecommerce.order.exception.InsufficientStockException;
import com.ecommerce.order.exception.ResourceNotFoundException;
import com.ecommerce.order.exception.ServiceUnavailableException;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.repository.OrderRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final ProductClient productClient;
    private final PaymentClient paymentClient;

    public OrderServiceImpl(OrderRepository orderRepository,
                            UserClient userClient,
                            ProductClient productClient,
                            PaymentClient paymentClient) {
        this.orderRepository = orderRepository;
        this.userClient = userClient;
        this.productClient = productClient;
        this.paymentClient = paymentClient;
    }

    @Override
    public OrderResponseDTO createOrder(OrderRequestDTO orderRequest) {
        log.info("Starting order placement workflow for User ID: {}, Product ID: {}, Quantity: {}",
                orderRequest.getUserId(), orderRequest.getProductId(), orderRequest.getQuantity());

        // STEP 1: Validate User by calling User Service via OpenFeign
        UserDTO user;
        try {
            log.info("Step 1: Calling User-Service to validate customer ID {}", orderRequest.getUserId());
            user = userClient.getUserById(orderRequest.getUserId());
            if (user == null) {
                throw new ResourceNotFoundException("User not found with id: " + orderRequest.getUserId());
            }
            log.info("Customer verified: {} ({})", user.getName(), user.getEmail());
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("User not found with id: " + orderRequest.getUserId());
        } catch (FeignException ex) {
            log.error("Failed to communicate with User-Service: {}", ex.getMessage());
            throw new ServiceUnavailableException("User-Service is temporarily unavailable. Please try again later.", ex);
        }

        // STEP 2: Check Product Availability by calling Product Service via OpenFeign
        ProductDTO product;
        try {
            log.info("Step 2: Calling Product-Service to verify item ID {}", orderRequest.getProductId());
            product = productClient.getProductById(orderRequest.getProductId());
            if (product == null) {
                throw new ResourceNotFoundException("Product not found with id: " + orderRequest.getProductId());
            }

            if (product.getQuantity() < orderRequest.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for product '" + product.getName() +
                        "'. Available: " + product.getQuantity() + ", Requested: " + orderRequest.getQuantity());
            }
            log.info("Product available: {} | Price: ${} | In Stock: {}",
                    product.getName(), product.getPrice(), product.getQuantity());
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Product not found with id: " + orderRequest.getProductId());
        } catch (FeignException ex) {
            log.error("Failed to communicate with Product-Service: {}", ex.getMessage());
            throw new ServiceUnavailableException("Product-Service is temporarily unavailable. Please try again later.", ex);
        }

        // STEP 3: Calculate Total Amount
        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(orderRequest.getQuantity()));
        log.info("Step 3: Calculated total amount: ${} ({} x ${})",
                totalAmount, orderRequest.getQuantity(), product.getPrice());

        // STEP 4: Reduce Product Stock in Product Service
        try {
            log.info("Step 4: Calling Product-Service to reduce stock by {}", orderRequest.getQuantity());
            productClient.reduceStock(orderRequest.getProductId(), orderRequest.getQuantity());
        } catch (FeignException ex) {
            log.error("Failed to reduce stock in Product-Service: {}", ex.getMessage());
            throw new ServiceUnavailableException("Unable to reserve product stock in Product-Service.", ex);
        }

        // Save order in PENDING status initially
        Order order = new Order(
                null,
                orderRequest.getUserId(),
                orderRequest.getProductId(),
                orderRequest.getQuantity(),
                totalAmount,
                "PENDING",
                LocalDateTime.now()
        );
        Order savedOrder = orderRepository.save(order);

        // STEP 5: Call Payment Service via OpenFeign
        PaymentResponseDTO paymentResponse;
        try {
            log.info("Step 5: Calling Payment-Service for Order ID {} (Amount: ${})", savedOrder.getId(), totalAmount);
            PaymentRequestDTO paymentRequest = new PaymentRequestDTO(
                    savedOrder.getId(),
                    totalAmount,
                    orderRequest.getPaymentMethod(),
                    orderRequest.getSimulatePaymentFailure()
            );
            paymentResponse = paymentClient.processPayment(paymentRequest);
            log.info("Payment response received: Status={}, Ref={}",
                    paymentResponse.getPaymentStatus(), paymentResponse.getTransactionReference());
        } catch (FeignException ex) {
            log.error("Failed to reach Payment-Service. Triggering stock compensation...", ex);
            // Compensate inventory
            productClient.restoreStock(orderRequest.getProductId(), orderRequest.getQuantity());
            savedOrder.setStatus("PAYMENT_SERVICE_UNAVAILABLE");
            orderRepository.save(savedOrder);
            throw new ServiceUnavailableException("Payment-Service is unreachable. Stock has been safely restored.", ex);
        }

        // STEP 6: Update Order Status based on Payment Result
        String finalMessage;
        if ("SUCCESS".equalsIgnoreCase(paymentResponse.getPaymentStatus())) {
            savedOrder.setStatus("CONFIRMED");
            finalMessage = "Order created and confirmed successfully!";
            log.info("Step 6: Order ID {} marked CONFIRMED.", savedOrder.getId());
        } else {
            // SAGA Compensation Pattern: Payment failed -> revert product stock!
            log.warn("Payment failed for Order ID {}. Compensating inventory stock...", savedOrder.getId());
            productClient.restoreStock(orderRequest.getProductId(), orderRequest.getQuantity());
            savedOrder.setStatus("PAYMENT_FAILED");
            finalMessage = "Order payment failed (" + paymentResponse.getMessage() + "). Inventory has been restored.";
        }

        Order updatedOrder = orderRepository.save(savedOrder);

        // STEP 7: Build and Return Complete Order Response
        return new OrderResponseDTO(
                updatedOrder.getId(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                product.getId(),
                product.getName(),
                updatedOrder.getQuantity(),
                updatedOrder.getTotalAmount(),
                updatedOrder.getStatus(),
                updatedOrder.getOrderDate(),
                paymentResponse.getTransactionReference(),
                paymentResponse.getPaymentStatus(),
                finalMessage
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        return enrichOrderDetails(order, "Order details retrieved successfully.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(order -> enrichOrderDetails(order, "Order retrieved."))
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponseDTO updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        order.setStatus(status.toUpperCase());
        Order updated = orderRepository.save(order);
        return enrichOrderDetails(updated, "Order status updated to " + status);
    }

    @Override
    public OrderResponseDTO cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            return enrichOrderDetails(order, "Order is already cancelled.");
        }

        // Restore product stock when a confirmed order is cancelled
        if ("CONFIRMED".equalsIgnoreCase(order.getStatus())) {
            try {
                log.info("Restoring stock for cancelled Order ID {}", id);
                productClient.restoreStock(order.getProductId(), order.getQuantity());
            } catch (Exception ex) {
                log.error("Failed to restore stock on cancellation: {}", ex.getMessage());
            }
        }

        order.setStatus("CANCELLED");
        Order saved = orderRepository.save(order);
        return enrichOrderDetails(saved, "Order cancelled successfully and product inventory restored.");
    }

    private OrderResponseDTO enrichOrderDetails(Order order, String message) {
        String userName = "Unknown";
        String userEmail = "Unknown";
        String productName = "Unknown";
        String paymentRef = "N/A";
        String paymentStatus = "N/A";

        // Query User Service
        try {
            UserDTO user = userClient.getUserById(order.getUserId());
            if (user != null) {
                userName = user.getName();
                userEmail = user.getEmail();
            }
        } catch (Exception e) {
            log.warn("Could not fetch user details for User ID {}: {}", order.getUserId(), e.getMessage());
        }

        // Query Product Service
        try {
            ProductDTO product = productClient.getProductById(order.getProductId());
            if (product != null) {
                productName = product.getName();
            }
        } catch (Exception e) {
            log.warn("Could not fetch product details for Product ID {}: {}", order.getProductId(), e.getMessage());
        }

        // Query Payment Service
        try {
            PaymentResponseDTO payment = paymentClient.getPaymentByOrderId(order.getId());
            if (payment != null) {
                paymentRef = payment.getTransactionReference();
                paymentStatus = payment.getPaymentStatus();
            }
        } catch (Exception e) {
            log.warn("Could not fetch payment details for Order ID {}: {}", order.getId(), e.getMessage());
        }

        return new OrderResponseDTO(
                order.getId(),
                order.getUserId(),
                userName,
                userEmail,
                order.getProductId(),
                productName,
                order.getQuantity(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getOrderDate(),
                paymentRef,
                paymentStatus,
                message
        );
    }
}
