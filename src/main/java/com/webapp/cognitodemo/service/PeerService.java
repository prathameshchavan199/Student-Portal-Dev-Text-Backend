package com.webapp.cognitodemo.service;

import com.webapp.cognitodemo.entity.User;
import com.webapp.cognitodemo.entity.peer.*;
import com.webapp.cognitodemo.repo.PeerProfileRepo;
import com.webapp.cognitodemo.repo.PeerReviewRepo;
import com.webapp.cognitodemo.repo.PeerSessionRequestRepo;
import com.webapp.cognitodemo.repo.PeerTopicRepo;
import com.webapp.cognitodemo.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/*
 * Backs the Peer to Peer module — students teaching each other. Covers both
 * flows from the "Teaching & Learning Flows" design: creating a teaching
 * profile, publishing topics, browsing/requesting sessions, accepting or
 * rejecting requests, tracking sessions to completion, and rating/review.
 *
 * Video conferencing is intentionally out of scope: sessions carry a plain
 * free-text meetingLink the teacher pastes in on accept (e.g. a Google Meet
 * link they created elsewhere) rather than any built-in calling.
 */
@Service
public class PeerService {

    @Autowired private PeerProfileRepo profileRepo;
    @Autowired private PeerTopicRepo topicRepo;
    @Autowired private PeerSessionRequestRepo sessionRepo;
    @Autowired private PeerReviewRepo reviewRepo;
    @Autowired private UserRepo userRepo;

    // ── auth helpers ────────────────────────────────────────────────────────

    private String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Not authenticated");
        }
        return auth.getName();
    }

    private String currentName() {
        return userRepo.findByEmail(currentEmail()).map(User::getFullName).orElse(null);
    }

    // ── profile ──────────────────────────────────────────────────────────────

    public Map<String, Object> getMyProfile() {
        return profileRepo.findByEmail(currentEmail()).map(this::toMap).orElse(null);
    }

    public Map<String, Object> saveProfile(PeerProfileRequest req) {
        String email = currentEmail();
        PeerProfile profile = profileRepo.findByEmail(email).orElseGet(() ->
                PeerProfile.builder().email(email).build());

        profile.setDisplayName(req.getDisplayName());
        profile.setBio(req.getBio());
        profile.setSubjects(req.getSubjects());
        profile.setSkills(req.getSkills());
        profile.setAvailability(req.getAvailability());
        profile.setExperience(req.getExperience());
        profile.setProficiencyLevel(req.getProficiencyLevel());

        profileRepo.save(profile);
        return toMap(profile);
    }

    // ── topics ───────────────────────────────────────────────────────────────

    public Map<String, Object> publishTopic(PeerTopicRequest req) {
        String email = currentEmail();
        PeerProfile profile = profileRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Create your teaching profile before publishing a topic"));

        PeerTopic topic = PeerTopic.builder()
                .teacherEmail(email)
                .teacherName(profile.getDisplayName())
                .title(req.getTitle())
                .description(req.getDescription())
                .level(req.getLevel())
                .tags(req.getTags())
                .availableDays(req.getAvailableDays())
                .timeSlotStart(req.getTimeSlotStart())
                .timeSlotEnd(req.getTimeSlotEnd())
                .sessionDuration(req.getSessionDuration())
                .status("PUBLISHED")
                .build();

        topicRepo.save(topic);
        return toMap(topic);
    }

    public List<Map<String, Object>> getMyTopics() {
        return topicRepo.findByTeacherEmailOrderByCreatedAtDesc(currentEmail())
                .stream().map(this::toMap).collect(Collectors.toList());
    }

    /* Browse: every published topic except the current user's own — you can't book yourself. */
    public List<Map<String, Object>> browseTopics(String search) {
        String email = currentEmail();
        String q = search == null ? "" : search.trim().toLowerCase();

        return topicRepo.findByStatusOrderByCreatedAtDesc("PUBLISHED")
                .stream()
                .filter(t -> !email.equalsIgnoreCase(t.getTeacherEmail()))
                .filter(t -> q.isEmpty()
                        || t.getTitle().toLowerCase().contains(q)
                        || (t.getDescription() != null && t.getDescription().toLowerCase().contains(q))
                        || (t.getTags() != null && t.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(q))))
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getTopicById(Long id) {
        PeerTopic topic = topicRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Topic not found"));
        return toMap(topic);
    }

    // ── sessions ─────────────────────────────────────────────────────────────

    public Map<String, Object> requestSession(PeerSessionRequestDto req) {
        String email = currentEmail();
        PeerTopic topic = topicRepo.findById(req.getTopicId())
                .orElseThrow(() -> new NoSuchElementException("Topic not found"));

        if (email.equalsIgnoreCase(topic.getTeacherEmail())) {
            throw new RuntimeException("You can't request a session on your own topic");
        }

        PeerSessionRequest session = PeerSessionRequest.builder()
                .topicId(topic.getId())
                .topicTitle(topic.getTitle())
                .teacherEmail(topic.getTeacherEmail())
                .teacherName(topic.getTeacherName())
                .learnerEmail(email)
                .learnerName(currentName())
                .requestedDate(req.getRequestedDate())
                .requestedTimeSlot(req.getRequestedTimeSlot())
                .message(req.getMessage())
                .status("PENDING")
                .build();

        sessionRepo.save(session);
        return toMap(session);
    }

    /* Pending requests waiting on the current user's response, as a teacher. */
    public List<Map<String, Object>> getIncomingRequests() {
        return sessionRepo.findByTeacherEmailAndStatusOrderByCreatedAtDesc(currentEmail(), "PENDING")
                .stream().map(this::toMap).collect(Collectors.toList());
    }

    public Map<String, Object> respondToRequest(Long id, PeerSessionRespondRequest req) {
        String email = currentEmail();
        PeerSessionRequest session = sessionRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Session request not found"));

        if (!email.equalsIgnoreCase(session.getTeacherEmail())) {
            throw new RuntimeException("Only the teacher for this session can respond to it");
        }
        if (!"PENDING".equals(session.getStatus())) {
            throw new RuntimeException("This request has already been responded to");
        }

        session.setStatus(Boolean.TRUE.equals(req.getAccept()) ? "ACCEPTED" : "REJECTED");
        session.setMeetingLink(req.getMeetingLink());
        session.setRespondedAt(LocalDateTime.now());

        sessionRepo.save(session);
        return toMap(session);
    }

    /* All sessions for the current user in a given role, bucketed by status —
     * matches the "My Sessions" tabs (Upcoming / Completed / Cancelled). */
    public Map<String, Object> getMySessions(String role) {
        String email = currentEmail();
        List<PeerSessionRequest> all = "teacher".equalsIgnoreCase(role)
                ? sessionRepo.findByTeacherEmailOrderByCreatedAtDesc(email)
                : sessionRepo.findByLearnerEmailOrderByCreatedAtDesc(email);

        List<Map<String, Object>> upcoming = all.stream()
                .filter(s -> "PENDING".equals(s.getStatus()) || "ACCEPTED".equals(s.getStatus()))
                .map(this::toMap).collect(Collectors.toList());
        List<Map<String, Object>> completed = all.stream()
                .filter(s -> "COMPLETED".equals(s.getStatus()))
                .map(this::toMap).collect(Collectors.toList());
        List<Map<String, Object>> cancelled = all.stream()
                .filter(s -> "CANCELLED".equals(s.getStatus()) || "REJECTED".equals(s.getStatus()))
                .map(this::toMap).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("upcoming", upcoming);
        result.put("completed", completed);
        result.put("cancelled", cancelled);
        return result;
    }

    public Map<String, Object> cancelSession(Long id) {
        String email = currentEmail();
        PeerSessionRequest session = sessionRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Session request not found"));

        if (!email.equalsIgnoreCase(session.getTeacherEmail()) && !email.equalsIgnoreCase(session.getLearnerEmail())) {
            throw new RuntimeException("You don't have access to this session");
        }
        if (!"PENDING".equals(session.getStatus()) && !"ACCEPTED".equals(session.getStatus())) {
            throw new RuntimeException("Only pending or accepted sessions can be cancelled");
        }

        session.setStatus("CANCELLED");
        sessionRepo.save(session);
        return toMap(session);
    }

    public Map<String, Object> completeSession(Long id) {
        String email = currentEmail();
        PeerSessionRequest session = sessionRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Session request not found"));

        if (!email.equalsIgnoreCase(session.getTeacherEmail()) && !email.equalsIgnoreCase(session.getLearnerEmail())) {
            throw new RuntimeException("You don't have access to this session");
        }
        if (!"ACCEPTED".equals(session.getStatus())) {
            throw new RuntimeException("Only an accepted session can be marked complete");
        }

        session.setStatus("COMPLETED");
        session.setCompletedAt(LocalDateTime.now());
        sessionRepo.save(session);
        return toMap(session);
    }

    // ── reviews ──────────────────────────────────────────────────────────────

    public Map<String, Object> submitReview(Long sessionId, PeerReviewRequest req) {
        String email = currentEmail();
        PeerSessionRequest session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session request not found"));

        if (!email.equalsIgnoreCase(session.getLearnerEmail())) {
            throw new RuntimeException("Only the learner of this session can leave a review");
        }
        if (!"COMPLETED".equals(session.getStatus())) {
            throw new RuntimeException("You can only review a completed session");
        }
        if (reviewRepo.findBySessionRequestId(sessionId).isPresent()) {
            throw new RuntimeException("This session has already been reviewed");
        }

        PeerReview review = PeerReview.builder()
                .sessionRequestId(sessionId)
                .teacherEmail(session.getTeacherEmail())
                .learnerEmail(email)
                .learnerName(currentName())
                .rating(req.getRating())
                .reviewText(req.getReviewText())
                .build();

        reviewRepo.save(review);
        return toReviewMap(review);
    }

    // ── mapping helpers ──────────────────────────────────────────────────────

    private double avgRatingFor(String teacherEmail) {
        List<PeerReview> reviews = reviewRepo.findByTeacherEmailOrderByCreatedAtDesc(teacherEmail);
        if (reviews.isEmpty()) return 0;
        return reviews.stream().mapToInt(PeerReview::getRating).average().orElse(0);
    }

    private int reviewCountFor(String teacherEmail) {
        return reviewRepo.findByTeacherEmailOrderByCreatedAtDesc(teacherEmail).size();
    }

    private Map<String, Object> toMap(PeerProfile p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("email", p.getEmail());
        m.put("displayName", p.getDisplayName());
        m.put("bio", p.getBio());
        m.put("subjects", p.getSubjects());
        m.put("skills", p.getSkills());
        m.put("availability", p.getAvailability());
        m.put("experience", p.getExperience());
        m.put("proficiencyLevel", p.getProficiencyLevel());
        m.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> toMap(PeerTopic t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("teacherEmail", t.getTeacherEmail());
        m.put("teacherName", t.getTeacherName());
        m.put("title", t.getTitle());
        m.put("description", t.getDescription());
        m.put("level", t.getLevel());
        m.put("tags", t.getTags());
        m.put("availableDays", t.getAvailableDays());
        m.put("timeSlotStart", t.getTimeSlotStart());
        m.put("timeSlotEnd", t.getTimeSlotEnd());
        m.put("sessionDuration", t.getSessionDuration());
        m.put("status", t.getStatus());
        m.put("rating", Math.round(avgRatingFor(t.getTeacherEmail()) * 10.0) / 10.0);
        m.put("reviewCount", reviewCountFor(t.getTeacherEmail()));
        m.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> toMap(PeerSessionRequest s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("topicId", s.getTopicId());
        m.put("topicTitle", s.getTopicTitle());
        m.put("teacherEmail", s.getTeacherEmail());
        m.put("teacherName", s.getTeacherName());
        m.put("learnerEmail", s.getLearnerEmail());
        m.put("learnerName", s.getLearnerName());
        m.put("requestedDate", s.getRequestedDate());
        m.put("requestedTimeSlot", s.getRequestedTimeSlot());
        m.put("message", s.getMessage());
        m.put("meetingLink", s.getMeetingLink());
        m.put("status", s.getStatus());
        m.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
        m.put("respondedAt", s.getRespondedAt() != null ? s.getRespondedAt().toString() : null);
        m.put("completedAt", s.getCompletedAt() != null ? s.getCompletedAt().toString() : null);
        m.put("reviewed", reviewRepo.findBySessionRequestId(s.getId()).isPresent());
        return m;
    }

    private Map<String, Object> toReviewMap(PeerReview r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("sessionRequestId", r.getSessionRequestId());
        m.put("teacherEmail", r.getTeacherEmail());
        m.put("learnerName", r.getLearnerName());
        m.put("rating", r.getRating());
        m.put("reviewText", r.getReviewText());
        m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        return m;
    }
}
