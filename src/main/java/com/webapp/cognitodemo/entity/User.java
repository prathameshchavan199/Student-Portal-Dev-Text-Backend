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

    @Column(name = "provider")
    private String provider = "LOCAL";

    /* STUDENT (default) or TPO_ADMIN — gates access to the TPO admin panel. */
    @Column(nullable = false, columnDefinition = "varchar(255) default 'STUDENT'")
    private String role = "STUDENT";

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






