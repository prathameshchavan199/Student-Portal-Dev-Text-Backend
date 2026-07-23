package com.webapp.cognitodemo.entity;

import lombok.Data;

@Data
public class SignupRequest {

    private String name;
    private String email;
    private String password;
//    private String mobile;
}