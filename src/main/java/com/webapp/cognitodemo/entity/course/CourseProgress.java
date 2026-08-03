package com.webapp.cognitodemo.entity.course;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "course_progress")
public class CourseProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail;
    private String courseId;
    private String courseName;

    /*
     * REGISTERED  – purchased, not yet started
     * IN_PROGRESS – learning has begun
     * COMPLETED   – all content finished
     * CERTIFIED   – certificate awarded
     */
    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private int progressPct;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
