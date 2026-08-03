package com.webapp.cognitodemo.entity.assessment;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "speaking_attempts")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SpeakingAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail;
    private String moduleId;
    private Long topicId;

    @Column(columnDefinition = "TEXT")
    private String topicText;

    @Column(columnDefinition = "TEXT")
    private String transcript;

    private int elapsedSecs;
    private int score;
    private int attemptNumber;
    private String badge;

    @Column(columnDefinition = "TEXT")
    private String skillsJson;

    @Column(columnDefinition = "TEXT")
    private String strengthsJson;

    @Column(columnDefinition = "TEXT")
    private String areasJson;

    private int wordCount;
    private int wpm;

    private LocalDateTime attemptedAt;

    @PrePersist
    void onCreate() {
        if (attemptedAt == null) attemptedAt = LocalDateTime.now();
    }
}
