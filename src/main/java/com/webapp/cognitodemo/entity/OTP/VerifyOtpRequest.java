package com.webapp.cognitodemo.entity.OTP;

import lombok.Data;

@Data
public class VerifyOtpRequest {

    private String email;

    private String otp;
}
