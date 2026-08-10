package com.webapp.cognitodemo.service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.Year;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${app.logo.url:https://studentportal.cyfenix.com/assets/Cyfenix-Logo-xqlkujei.png}")
    private String logoUrl;

    private static final String SUPPORT_EMAIL = "support@cyfenix.com";
    private static final String EXPIRY_MINUTES = "5";

    private String otpTemplate;

    @PostConstruct
    private void loadTemplate() throws Exception {
        ClassPathResource resource = new ClassPathResource("templates/otp-email.html");
        otpTemplate = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    // Called from signup flow — name is available
    public void sendOtpEmail(String toEmail, String otp, String recipientName) throws Exception {
        String firstName = (recipientName != null && !recipientName.isBlank())
                ? recipientName.trim().split("\\s+")[0]
                : "there";
        sendOtpEmailInternal(toEmail, otp, firstName);
    }

    // Called from forgot-password flow — no name available
    public void sendOtpEmail(String toEmail, String otp) throws Exception {
        sendOtpEmailInternal(toEmail, otp, "there");
    }

    private void sendOtpEmailInternal(String toEmail, String otp, String firstName) throws Exception {
        String[] digits = otp.split("");

        String html = buildHtml(firstName, otp, digits);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(senderEmail);
        helper.setTo(toEmail);
        helper.setSubject("Your Student Portal verification code");
        helper.setText(html, true);

        mailSender.send(message);
    }

    private String buildHtml(String name, String otp, String[] digits) {
        return otpTemplate
                .replace("{{name}}", escape(name))
                .replace("{{otpCode}}", escape(otp))
                .replace("{{otpDigit1}}", escape(digits[0]))
                .replace("{{otpDigit2}}", escape(digits[1]))
                .replace("{{otpDigit3}}", escape(digits[2]))
                .replace("{{otpDigit4}}", escape(digits[3]))
                .replace("{{otpDigit5}}", escape(digits[4]))
                .replace("{{otpDigit6}}", escape(digits[5]))
                .replace("{{expiryMinutes}}", EXPIRY_MINUTES)
                .replace("{{supportEmail}}", SUPPORT_EMAIL)
                .replace("{{year}}", String.valueOf(Year.now().getValue()))
                .replace("{{logoUrl}}", logoUrl);
    }

    private static String escape(String s) {
        return s == null ? "" : s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
