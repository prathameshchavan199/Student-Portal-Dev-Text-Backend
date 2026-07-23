package com.webapp.cognitodemo.entity;

import lombok.Data;

@Data
public class LoginRequest {

    private String email;
    private String password;
}
