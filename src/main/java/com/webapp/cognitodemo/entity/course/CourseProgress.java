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

    /*
     * JSON array of completed lesson keys, e.g. ["0-0","0-1","1-0"]
     * where each key is "{moduleIndex}-{lessonIndex}".
     */
    @Column(columnDefinition = "TEXT")
    private String completedLessonsJson;

    /* Key of the last lesson the learner opened, e.g. "0-1" — used to resume playback. */
    private String lastLessonKey;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
