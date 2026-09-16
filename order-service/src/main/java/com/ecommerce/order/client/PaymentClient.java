package com.ecommerce.order.client;

import com.ecommerce.order.dto.PaymentRequestDTO;
import com.ecommerce.order.dto.PaymentResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * OpenFeign Client for synchronous REST communication with Payment Service.
 * Resolves the service instance dynamically via Eureka Service Discovery (PAYMENT-SERVICE).
 */
@FeignClient(name = "payment-service")
public interface PaymentClient {

    @PostMapping("/api/payments/process")
    PaymentResponseDTO processPayment(@RequestBody PaymentRequestDTO request);

    @GetMapping("/api/payments/order/{orderId}")
    PaymentResponseDTO getPaymentByOrderId(@PathVariable("orderId") Long orderId);
}
