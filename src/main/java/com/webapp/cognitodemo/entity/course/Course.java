package com.webapp.cognitodemo.entity.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "courses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Integer price;

    private String duration;
    private String level;
    private String imageUrl;
    private String instructor;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String courseArea;
    private String topic;
    private String format;

    @Column(name = "course_date")
    private String date;

    @Column(name = "course_time")
    private String time;

    private String platform;
    private String location;
    private String startsIn;
    private Integer seatsLeft;
    private String accent;

    @Column(columnDefinition = "TEXT")
    private String sessionsJson;

    @Column(columnDefinition = "TEXT")
    private String aboutCourse;

    // JSON array of strings: ["bullet 1", "bullet 2", ...]
    @Column(columnDefinition = "TEXT")
    private String youWillLearnJson;

    // JSON array: [{ "title": "Module 1", "lessons": ["Lesson A", "Lesson B"] }, ...]
    @Column(columnDefinition = "TEXT")
    private String curriculumJson;
}
