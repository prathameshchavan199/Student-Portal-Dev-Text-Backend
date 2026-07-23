package com.webapp.cognitodemo.service;

import com.webapp.cognitodemo.entity.OTP.OtpData;
import com.webapp.cognitodemo.entity.SignupRequest;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private final Map<String, OtpData> otpStore =
            new ConcurrentHashMap<>();

    private final Map<String, SignupRequest> pendingSignups =
            new ConcurrentHashMap<>();

    private static final long OTP_EXPIRY =
            5 * 60 * 1000; // 5 mins

    public String generateOtp(
            String email) {

        String otp =
                String.valueOf(
                        100000 +
                                new Random().nextInt(900000)
                );

        otpStore.put(
                email,
                new OtpData(
                        otp,
                        System.currentTimeMillis()
                )
        );

        return otp;
    }

    public boolean verifyOtp(
            String email,
            String enteredOtp) {

        OtpData otpData =
                otpStore.get(email);

        if (otpData == null) {
            return false;
        }

        long age =
                System.currentTimeMillis()
                        - otpData.getCreatedAt();

        if (age > OTP_EXPIRY) {

            otpStore.remove(email);

            return false;
        }

        return otpData.getOtp()
                .equals(enteredOtp);
    }

    public void removeOtp(
            String email) {

        otpStore.remove(email);
    }

    public void storePendingSignup(
            String email,
            SignupRequest request) {

        pendingSignups.put(email, request);
    }

    public SignupRequest getPendingSignup(
            String email) {

        return pendingSignups.get(email);
    }

    public void removePendingSignup(
            String email) {

        pendingSignups.remove(email);
    }
}




//package com.webapp.cognitodemo.service;

//import org.springframework.stereotype.Service;
//
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Service
//public class OtpService {
//
//    private final Map<String, String> otpStore =
//            new ConcurrentHashMap<>();
//
//    public String generateOtp(String email) {
//
//        String otp =
//                String.valueOf(
//                        (int)(100000 + Math.random() * 900000)
//                );
//
//        otpStore.put(
//                email,
//                otp
//        );
//
//        return otp;
//    }
//
//    public boolean verifyOtp(
//            String email,
//            String otp) {
//
//        return otp.equals(
//                otpStore.get(email)
//        );
//    }
//}
