package com.webapp.cognitodemo.controler;

import com.webapp.cognitodemo.entity.assessment.SpeakingSubmitRequest;
import com.webapp.cognitodemo.entity.assessment.WritingSubmitRequest;
import com.webapp.cognitodemo.service.CommunicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/communication")
public class CommunicationController {

    @Autowired private CommunicationService service;

    // ── Topics ─────────────────────────────────────────────────────────────────

    @GetMapping("/speaking/topic/random")
    public ResponseEntity<?> randomSpeakingTopic() {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", service.getRandomTopic("speaking")));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/writing/prompt/random")
    public ResponseEntity<?> randomWritingPrompt() {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", service.getRandomTopic("writing")));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── Submissions ────────────────────────────────────────────────────────────

    @PostMapping("/speaking/submit")
    public ResponseEntity<?> submitSpeaking(
            Authentication auth,
            @RequestBody SpeakingSubmitRequest req,
            @RequestParam(defaultValue = "speaking-test") String moduleId) {
        try {
            return ResponseEntity.ok(Map.of("success", true,
                "data", service.submitSpeaking(auth.getName(), moduleId, req)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/writing/submit")
    public ResponseEntity<?> submitWriting(
            Authentication auth,
            @RequestBody WritingSubmitRequest req,
            @RequestParam(defaultValue = "writing-test") String moduleId) {
        try {
            return ResponseEntity.ok(Map.of("success", true,
                "data", service.submitWriting(auth.getName(), moduleId, req)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
