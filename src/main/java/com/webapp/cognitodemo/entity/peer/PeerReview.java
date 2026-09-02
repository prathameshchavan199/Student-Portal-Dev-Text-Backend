package com.webapp.cognitodemo.entity.peer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/* A learner's rating + review of a completed PeerSessionRequest — "Rate & Review" step. */
@Entity
@Table(name = "peer_reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeerReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long sessionRequestId;

    @Column(nullable = false)
    private String teacherEmail;

    @Column(nullable = false)
    private String learnerEmail;
    private String learnerName;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 1000)
    private String reviewText;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
