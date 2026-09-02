package com.webapp.cognitodemo.controler;

import com.webapp.cognitodemo.entity.peer.*;
import com.webapp.cognitodemo.service.PeerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

/*
 * Peer to Peer module — students teaching and learning from each other.
 * All endpoints require a logged-in student (no separate role; anyone can
 * be both a teacher and a learner). See PeerService for the flow this backs.
 */
@Tag(name = "Peer to Peer", description = "Student-to-student teaching and learning: profiles, topics, session requests, reviews")
@RestController
@RequestMapping("/api/peer")
public class PeerController {

    @Autowired private PeerService peerService;

    private ResponseEntity<?> ok(Object data) {
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    private ResponseEntity<?> fail(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("success", false, "message", message));
    }

    // ── profile ──────────────────────────────────────────────────────────────

    @Operation(summary = "Get the current user's teaching profile, or null if they haven't created one")
    @GetMapping("/profile/me")
    public ResponseEntity<?> getMyProfile() {
        return ok(peerService.getMyProfile());
    }

    @Operation(summary = "Create or update the current user's teaching profile")
    @PostMapping("/profile")
    public ResponseEntity<?> saveProfile(@Valid @RequestBody PeerProfileRequest req) {
        return ok(peerService.saveProfile(req));
    }

    // ── topics ───────────────────────────────────────────────────────────────

    @Operation(summary = "Publish a new topic to teach (requires a teaching profile)")
    @PostMapping("/topics")
    public ResponseEntity<?> publishTopic(@Valid @RequestBody PeerTopicRequest req) {
        try {
            return ok(peerService.publishTopic(req));
        } catch (RuntimeException e) {
            return fail(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @Operation(summary = "List the current user's own published topics")
    @GetMapping("/topics/mine")
    public ResponseEntity<?> getMyTopics() {
        return ok(peerService.getMyTopics());
    }

    @Operation(summary = "Browse other students' published topics, optionally filtered by a search term")
    @GetMapping("/topics")
    public ResponseEntity<?> browseTopics(@RequestParam(required = false) String search) {
        return ok(peerService.browseTopics(search));
    }

    @Operation(summary = "Get a single topic's details")
    @GetMapping("/topics/{id}")
    public ResponseEntity<?> getTopic(@PathVariable Long id) {
        try {
            return ok(peerService.getTopicById(id));
        } catch (NoSuchElementException e) {
            return fail(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    // ── sessions ─────────────────────────────────────────────────────────────

    @Operation(summary = "Request a session on a topic")
    @PostMapping("/sessions")
    public ResponseEntity<?> requestSession(@Valid @RequestBody PeerSessionRequestDto req) {
        try {
            return ok(peerService.requestSession(req));
        } catch (NoSuchElementException e) {
            return fail(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (RuntimeException e) {
            return fail(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @Operation(summary = "List pending session requests waiting on the current user, as a teacher")
    @GetMapping("/sessions/requests")
    public ResponseEntity<?> getIncomingRequests() {
        return ok(peerService.getIncomingRequests());
    }

    @Operation(summary = "Accept or reject a pending session request (teacher only)")
    @PutMapping("/sessions/{id}/respond")
    public ResponseEntity<?> respond(@PathVariable Long id, @Valid @RequestBody PeerSessionRespondRequest req) {
        try {
            return ok(peerService.respondToRequest(id, req));
        } catch (NoSuchElementException e) {
            return fail(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (RuntimeException e) {
            return fail(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @Operation(summary = "List the current user's sessions, bucketed into upcoming/completed/cancelled")
    @GetMapping("/sessions/mine")
    public ResponseEntity<?> getMySessions(@RequestParam(defaultValue = "learner") String role) {
        return ok(peerService.getMySessions(role));
    }

    @Operation(summary = "Cancel a pending or accepted session (teacher or learner)")
    @PutMapping("/sessions/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        try {
            return ok(peerService.cancelSession(id));
        } catch (NoSuchElementException e) {
            return fail(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (RuntimeException e) {
            return fail(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @Operation(summary = "Mark an accepted session as complete (teacher or learner)")
    @PutMapping("/sessions/{id}/complete")
    public ResponseEntity<?> complete(@PathVariable Long id) {
        try {
            return ok(peerService.completeSession(id));
        } catch (NoSuchElementException e) {
            return fail(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (RuntimeException e) {
            return fail(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // ── reviews ──────────────────────────────────────────────────────────────

    @Operation(summary = "Rate and review a completed session (learner only)")
    @PostMapping("/sessions/{id}/review")
    public ResponseEntity<?> review(@PathVariable Long id, @Valid @RequestBody PeerReviewRequest req) {
        try {
            return ok(peerService.submitReview(id, req));
        } catch (NoSuchElementException e) {
            return fail(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (RuntimeException e) {
            return fail(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
