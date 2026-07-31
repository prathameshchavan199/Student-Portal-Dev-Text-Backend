package com.webapp.cognitodemo.entity.assessment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_attempts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String moduleId;

    private int score;
    private int total;
    private int attemptNumber;
    private LocalDateTime attemptedAt;

    @PrePersist
    void onCreate() {
        if (attemptedAt == null) attemptedAt = LocalDateTime.now();
    }
}
