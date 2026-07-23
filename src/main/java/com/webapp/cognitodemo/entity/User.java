package com.webapp.cognitodemo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Entity
@Table(name = "usersbycognito")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Full name is required")
    @Column(nullable = false)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean registration = false;

    public User() {
    }

    public User(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
    }

    public void setName(String name) {
        this.email = name;
    }
}






//package com.webapp.cognitodemo.entity;
//
//import jakarta.persistence.*;
//import jakarta.validation.constraints.Email;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.Size;
//import lombok.Data;
//
//@Data
//@Entity
//@Table(name = "users")
//public class User {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @NotBlank(message = "Full name is required")
//    @Column(nullable = false)
//    private String fullName;
//
//    @NotBlank(message = "Email is required")
//    @Email(message = "Invalid email address")
//    @Column(nullable = false, unique = true)
//    private String email;
//
//    @NotBlank(message = "Password is required")
//    @Size(min = 8, message = "Password must be at least 8 characters")
//    @Column(nullable = false)
//    private String password;
//
//    // Constructors
//    public User() {
//    }
//
//    public User(String fullName, String email, String password) {
//        this.fullName = fullName;
//        this.email = email;
//        this.password = password;
//    }
//
//    // Getters and Setters
//
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public String getFullName() {
//        return fullName;
//    }
//
//    public void setFullName(String fullName) {
//        this.fullName = fullName;
//    }
//
//    public String getEmail() {
//        return email;
//    }
//
//    public void setEmail(String email) {
//        this.email = email;
//    }
//
//    public String getPassword() {
//        return password;
//    }
//
//    public void setPassword(String password) {
//        this.password = password;
//    }
//}