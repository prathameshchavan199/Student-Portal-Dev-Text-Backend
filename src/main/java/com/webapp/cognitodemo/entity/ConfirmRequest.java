package com.webapp.cognitodemo.entity;

import lombok.Data;

@Data
public class ConfirmRequest {

    private String email;
    private String otp;
}
