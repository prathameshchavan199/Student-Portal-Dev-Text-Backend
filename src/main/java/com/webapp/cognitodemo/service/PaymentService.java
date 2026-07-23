package com.webapp.cognitodemo.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

import com.webapp.cognitodemo.entity.payment.CreateOrderRequest;
import com.webapp.cognitodemo.entity.payment.Payment;
import com.webapp.cognitodemo.entity.payment.VerifyPaymentRequest;
import com.webapp.cognitodemo.repo.PaymentRepo;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentService {

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private PaymentRepo paymentRepo;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    /*
     * Expose the public key id so the frontend can open the
     * Razorpay checkout without hard-coding it.
     */
    public String getKeyId() {
        return keyId;
    }

    /*
     * Creates a Razorpay order for the exact course amount sent
     * from the frontend, persists a CREATED record, and returns
     * the order so the frontend can launch the checkout.
     */
    public Order createOrder(
            CreateOrderRequest request,
            String userEmail) throws RazorpayException {

        // Razorpay works in the smallest currency unit (paise).
        // e.g. 499.00 INR -> 49900 paise
        long amountInPaise =
                request.getAmount()
                        .multiply(BigDecimal.valueOf(100))
                        .longValueExact();

        String currency =
                (request.getCurrency() == null
                        || request.getCurrency().isBlank())
                        ? "INR"
                        : request.getCurrency();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", currency);
        orderRequest.put(
                "receipt",
                "rcpt_" + System.currentTimeMillis()
        );

        Order order =
                razorpayClient.Orders.create(orderRequest);

        Payment payment = new Payment();
        payment.setRazorpayOrderId(order.get("id"));
        payment.setCourseId(request.getCourseId());
        payment.setCourseName(request.getCourseName());
        payment.setAmount(amountInPaise);
        payment.setCurrency(currency);
        payment.setStatus("CREATED");
        payment.setUserEmail(userEmail);

        paymentRepo.save(payment);

        return order;
    }

    /*
     * Verifies the signature returned by the Razorpay checkout.
     * The signature is an HMAC of "orderId|paymentId" keyed with
     * the secret, so a valid signature proves the payment really
     * came from Razorpay and was not tampered with.
     */
    public boolean verifyPayment(
            VerifyPaymentRequest request) throws RazorpayException {

        JSONObject options = new JSONObject();
        options.put(
                "razorpay_order_id",
                request.getRazorpayOrderId()
        );
        options.put(
                "razorpay_payment_id",
                request.getRazorpayPaymentId()
        );
        options.put(
                "razorpay_signature",
                request.getRazorpaySignature()
        );

        boolean valid =
                Utils.verifyPaymentSignature(options, keySecret);

        Payment payment =
                paymentRepo
                        .findByRazorpayOrderId(
                                request.getRazorpayOrderId()
                        )
                        .orElse(null);

        if (payment != null) {

            payment.setRazorpayPaymentId(
                    request.getRazorpayPaymentId()
            );
            payment.setRazorpaySignature(
                    request.getRazorpaySignature()
            );
            payment.setStatus(valid ? "PAID" : "FAILED");

            paymentRepo.save(payment);
        }

        return valid;
    }
}
