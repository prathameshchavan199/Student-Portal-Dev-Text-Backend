package com.webapp.cognitodemo.entity;

import lombok.Data;

@Data
public class LoginResponse {

    private String idToken;

    private String refreshToken;

    private String email;

    private String name;

    private boolean registered;

}