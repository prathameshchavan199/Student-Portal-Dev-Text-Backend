//package com.webapp.cognitodemo.entity.peer;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.time.LocalDateTime;
//
///*
// * A learner's request for a session on a PeerTopic — covers "Request a
// * Session" through "Complete Session" in both flows.
// *
// * Video conferencing itself is out of scope for now: meetingLink is just a
// * free-text field the teacher can paste a link into (e.g. a Google Meet
// * link they created elsewhere) when accepting, matching the "Share Session
// * Link" step in the flow without building real conferencing.
// */
//@Entity
//@Table(name = "peer_session_requests")
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class PeerSessionRequest {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false)
//    private Long topicId;
//
//    /* Denormalized snapshot of the topic/teacher/learner at request time, so
//     * this record stays meaningful even if the topic is later edited or removed. */
//    private String topicTitle;
//
//    @Column(nullable = false)
//    private String teacherEmail;
//    private String teacherName;
//
//    @Column(nullable = false)
//    private String learnerEmail;
//    private String learnerName;
//
//    @Column(nullable = false)
//    private String requestedDate;
//
//    private String requestedTimeSlot;
//
//    @Column(length = 1000)
//    private String message;
//
//    /* Pasted in by the teacher on accept — see class comment. */
//    private String meetingLink;
//
//    /* PENDING / ACCEPTED / REJECTED / CANCELLED / COMPLETED */
//    @Column(nullable = false)
//    private String status;
//
//    @Column(nullable = false)
//    private LocalDateTime createdAt;
//    private LocalDateTime respondedAt;
//    private LocalDateTime completedAt;
//
//    @PrePersist
//    void onCreate() {
//        if (createdAt == null) createdAt = LocalDateTime.now();
//        if (status == null || status.isBlank()) status = "PENDING";
//    }
//}


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
 * Status lifecycle: PENDING_TPO_APPROVAL (initial) -> PENDING (once the
 * learner's college TPO admin approves it, only then does the teacher even
 * see it in their incoming requests) -> ACCEPTED/REJECTED (by teacher) ->
 * COMPLETED. A TPO admin can also reject at the first gate, which sets
 * REJECTED_BY_TPO and ends the flow without ever reaching the teacher.
 * CANCELLED can happen from PENDING_TPO_APPROVAL, PENDING, or ACCEPTED.
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

    /* PENDING_TPO_APPROVAL / PENDING / ACCEPTED / REJECTED / REJECTED_BY_TPO / CANCELLED / COMPLETED */
    @Column(nullable = false)
    private String status;

    /* TPO gate: null = not yet reviewed, true = approved, false = rejected.
     * Kept separate from `status` so the "already TPO-approved" history
     * (Teacher Student Requests tab) stays queryable even after the session
     * later moves on to ACCEPTED/COMPLETED/CANCELLED. */
    private Boolean tpoApproved;
    private String tpoAdminEmail;
    private LocalDateTime tpoRespondedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
    private LocalDateTime completedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null || status.isBlank()) status = "PENDING_TPO_APPROVAL";
    }
}
