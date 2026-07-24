package com.webapp.cognitodemo.controler;

import com.razorpay.Order;

import com.webapp.cognitodemo.entity.payment.CreateOrderRequest;
import com.webapp.cognitodemo.entity.payment.VerifyPaymentRequest;
import com.webapp.cognitodemo.service.PaymentService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /*
     * CREATE ORDER
     *
     * The frontend sends the amount of the selected course. A
     * Razorpay order is created for that exact amount and the
     * order details (plus the public key id) are returned so the
     * checkout can be opened on the client.
     */
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {

        try {

            String email =
                    (authentication != null)
                            ? authentication.getName()
                            : null;

            Order order =
                    paymentService.createOrder(request, email);

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "keyId", paymentService.getKeyId(),
                            "orderId", order.get("id"),
                            "amount", order.get("amount"),
                            "currency", order.get("currency")
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(
                            Map.of(
                                    "success", false,
                                    "message",
                                    "Failed to create payment order"
                            )
                    );
        }
    }

    /*
     * VERIFY PAYMENT
     *
     * Called after a successful checkout. The signature is
     * validated server-side before the purchase is treated as
     * complete.
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {

        try {

            boolean valid =
                    paymentService.verifyPayment(request);

            if (!valid) {

                return ResponseEntity.badRequest()
                        .body(
                                Map.of(
                                        "success", false,
                                        "message",
                                        "Payment verification failed"
                                )
                        );
            }

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message",
                            "Payment verified successfully"
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(
                            Map.of(
                                    "success", false,
                                    "message",
                                    "Could not verify payment"
                            )
                    );
        }
    }
}
