package com.webapp.cognitodemo.entity.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderRequest {

    /*
     * The course amount as shown on the frontend, in the major
     * currency unit (e.g. rupees: 499.00). It is converted to
     * paise before being sent to Razorpay.
     */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    /*
     * Defaults to INR when the frontend does not specify one.
     */
    private String currency = "INR";

    private Long courseId;

    private String courseName;
}
