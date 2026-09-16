package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.PaymentRequestDTO;
import com.ecommerce.payment.dto.PaymentResponseDTO;
import com.ecommerce.payment.exception.ResourceNotFoundException;
import com.ecommerce.payment.model.Payment;
import com.ecommerce.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private final PaymentRepository paymentRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public PaymentResponseDTO processPayment(PaymentRequestDTO requestDTO) {
        String txRef = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Processing payment for order ID {} of amount ${}, Method: {}",
                requestDTO.getOrderId(), requestDTO.getAmount(), requestDTO.getPaymentMethod());

        // Simple mock payment rule:
        // Fails if simulateFailure is true or amount exceeds 50000.00
        boolean isFailed = Boolean.TRUE.equals(requestDTO.getSimulateFailure()) ||
                requestDTO.getAmount().compareTo(new BigDecimal("50000.00")) > 0;

        String status = isFailed ? "FAILED" : "SUCCESS";
        String message = isFailed ?
                "Payment transaction declined by mock payment gateway." :
                "Payment processed successfully.";

        Payment payment = new Payment(
                null,
                requestDTO.getOrderId(),
                requestDTO.getAmount(),
                status,
                requestDTO.getPaymentMethod(),
                txRef,
                LocalDateTime.now()
        );

        Payment saved = paymentRepository.save(payment);
        log.info("Payment record saved with ID {}, Reference {}, Status {}", saved.getId(), txRef, status);

        return new PaymentResponseDTO(
                saved.getId(),
                saved.getOrderId(),
                saved.getAmount(),
                saved.getPaymentStatus(),
                saved.getPaymentMethod(),
                saved.getTransactionReference(),
                saved.getPaymentDate(),
                message
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        return mapToDTO(payment, "Payment record found.");
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentByOrderId(Long orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        if (payments.isEmpty()) {
            throw new ResourceNotFoundException("No payment found for order id: " + orderId);
        }
        // Return the latest payment attempt for this order
        Payment latestPayment = payments.get(payments.size() - 1);
        return mapToDTO(latestPayment, "Latest payment record for order retrieved.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(p -> mapToDTO(p, "Payment retrieved."))
                .collect(Collectors.toList());
    }

    private PaymentResponseDTO mapToDTO(Payment payment, String message) {
        return new PaymentResponseDTO(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getPaymentStatus(),
                payment.getPaymentMethod(),
                payment.getTransactionReference(),
                payment.getPaymentDate(),
                message
        );
    }
}
