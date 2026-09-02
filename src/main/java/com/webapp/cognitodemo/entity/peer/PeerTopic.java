package com.webapp.cognitodemo.entity.peer;

import com.webapp.cognitodemo.converter.StringListConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/*
 * A topic a student has published to teach, from the "Create Topic" /
 * "Topic Published" steps of the teaching flow. Learners browse and
 * request sessions against these.
 */
@Entity
@Table(name = "peer_topics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeerTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String teacherEmail;

    /* Denormalized so listings don't need a join back to PeerProfile. */
    private String teacherName;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    /* Beginner / Intermediate / Advanced */
    private String level;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> tags;

    /* Days the teacher is available for this topic, e.g. ["Mon","Wed","Fri"] */
    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> availableDays;

    private String timeSlotStart;
    private String timeSlotEnd;

    /* e.g. "1 Hour" */
    private String sessionDuration;

    /* PUBLISHED / UNPUBLISHED — topics are published as soon as they're created;
     * unpublishing just hides a topic from Browse without deleting its history. */
    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null || status.isBlank()) status = "PUBLISHED";
    }
}
