package com.webapp.cognitodemo.service;

import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public void sendOtpEmail(
            String toEmail,
            String otp)
            throws Exception {

        MimeMessage message =
                mailSender.createMimeMessage();

        MimeMessageHelper helper =
                new MimeMessageHelper(
                        message,
                        true
                );

        helper.setFrom(senderEmail);
        helper.setTo(toEmail);

        helper.setSubject(
                "Email Verification OTP"
        );

        helper.setText(
                "<h2>Your OTP is: "
                        + otp +
                        "</h2>" +
                        "<p>This OTP will expire in 5 minutes.</p>",
                true
        );

        mailSender.send(message);
    }
}


//package com.webapp.cognitodemo.service;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import sendinblue.ApiClient;
//import sendinblue.Configuration;
//import sibApi.TransactionalEmailsApi;
//import sibModel.SendSmtpEmail;
//import sibModel.SendSmtpEmailSender;
//import sibModel.SendSmtpEmailTo;
//
//import java.util.List;
//
//@Service
//public class EmailService {
//
//    @Value("${brevo.api.key}")
//    private String apiKey;
//
//    @Value("${brevo.sender.email}")
//    private String senderEmail;
//
//    @Value("${brevo.sender.name}")
//    private String senderName;
//
//
//    public void sendOtpEmail(
//            String toEmail,
//            String otp) throws Exception {
//
//        ApiClient client =
//                Configuration.getDefaultApiClient();
//
//        client.setApiKey(
//                apiKey
//        );
//
//        System.out.println("API key present: " + (apiKey));
//
//        TransactionalEmailsApi api =
//                new TransactionalEmailsApi(
//                        client
//                );
//
//        SendSmtpEmail email =
//                new SendSmtpEmail();
//
//        email.setSender(
//                new SendSmtpEmailSender()
//                        .email(senderEmail)
//                        .name(senderName)
//        );
//
//        email.setTo(
//                List.of(
//                        new SendSmtpEmailTo()
//                                .email(toEmail)
//                )
//        );
//
//        email.setSubject(
//                "Email Verification OTP"
//        );
//
//        email.setHtmlContent(
//                "<h2>Your OTP is "
//                        + otp
//                        + "</h2>"
//        );
//
//        api.sendTransacEmail(
//                email
//        );
//    }
//}