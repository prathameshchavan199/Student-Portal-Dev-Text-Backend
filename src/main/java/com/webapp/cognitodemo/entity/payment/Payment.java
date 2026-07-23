package com.webapp.cognitodemo.entity.payment;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Razorpay identifiers
     */
    @Column(nullable = false, unique = true)
    private String razorpayOrderId;

    private String razorpayPaymentId;

    private String razorpaySignature;

    /*
     * Course this payment is for
     */
    private Long courseId;

    private String courseName;

    /*
     * Amount in the smallest currency unit (paise for INR),
     * exactly as it was sent to Razorpay.
     */
    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private String currency;

    /*
     * CREATED -> order created, awaiting payment
     * PAID    -> payment captured and signature verified
     * FAILED  -> signature verification failed
     */
    @Column(nullable = false)
    private String status;

    private String userEmail;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
