package com.webapp.cognitodemo.entity.peer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * A learner's request for a session on a PeerTopic — covers "Request a
 * Session" through "Complete Session" in both flows.
 *
 * Video conferencing itself is out of scope for now: meetingLink is just a
 * free-text field the teacher can paste a link into (e.g. a Google Meet
 * link they created elsewhere) when accepting, matching the "Share Session
 * Link" step in the flow without building real conferencing.
 */
@Entity
@Table(name = "peer_session_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeerSessionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long topicId;

    /* Denormalized snapshot of the topic/teacher/learner at request time, so
     * this record stays meaningful even if the topic is later edited or removed. */
    private String topicTitle;

    @Column(nullable = false)
    private String teacherEmail;
    private String teacherName;

    @Column(nullable = false)
    private String learnerEmail;
    private String learnerName;

    @Column(nullable = false)
    private String requestedDate;

    private String requestedTimeSlot;

    @Column(length = 1000)
    private String message;

    /* Pasted in by the teacher on accept — see class comment. */
    private String meetingLink;

    /* PENDING / ACCEPTED / REJECTED / CANCELLED / COMPLETED */
    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
    private LocalDateTime completedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null || status.isBlank()) status = "PENDING";
    }
}
