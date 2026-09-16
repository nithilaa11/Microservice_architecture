package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.PaymentRequestDTO;
import com.ecommerce.payment.dto.PaymentResponseDTO;

import java.util.List;

public interface PaymentService {
    PaymentResponseDTO processPayment(PaymentRequestDTO requestDTO);
    PaymentResponseDTO getPaymentById(Long id);
    PaymentResponseDTO getPaymentByOrderId(Long orderId);
    List<PaymentResponseDTO> getAllPayments();
}
