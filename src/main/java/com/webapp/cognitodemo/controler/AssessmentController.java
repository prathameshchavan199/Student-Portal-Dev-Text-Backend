package com.webapp.cognitodemo.controler;

import com.webapp.cognitodemo.entity.assessment.*;
import com.webapp.cognitodemo.service.AssessmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {

    @Autowired
    private AssessmentService assessmentService;

    // ── Categories ───────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<?> getAllCategories() {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", assessmentService.getAllCategories()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<?> getCategory(@PathVariable String categoryId) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", assessmentService.getCategoryWithModules(categoryId)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody AssessmentCategoryRequest req) {
        try {
            return ResponseEntity.status(201).body(Map.of("success", true, "data", assessmentService.createCategory(req)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<?> updateCategory(@PathVariable String categoryId, @RequestBody AssessmentCategoryRequest req) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", assessmentService.updateCategory(categoryId, req)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<?> deleteCategory(@PathVariable String categoryId) {
        try {
            assessmentService.deleteCategory(categoryId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Category deleted"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── Modules ──────────────────────────────────────────────────────────────

    @GetMapping("/{categoryId}/modules")
    public ResponseEntity<?> getModules(@PathVariable String categoryId) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", assessmentService.getModulesForCategory(categoryId)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/modules")
    public ResponseEntity<?> createModule(@RequestBody AssessmentModuleRequest req) {
        try {
            return ResponseEntity.status(201).body(Map.of("success", true, "data", assessmentService.createModule(req)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/modules/{moduleId}")
    public ResponseEntity<?> updateModule(@PathVariable String moduleId, @RequestBody AssessmentModuleRequest req) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", assessmentService.updateModule(moduleId, req)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/modules/{moduleId}")
    public ResponseEntity<?> deleteModule(@PathVariable String moduleId) {
        try {
            assessmentService.deleteModule(moduleId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Module deleted"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── MCQ Questions ─────────────────────────────────────────────────────────

    @GetMapping("/{categoryId}/questions")
    public ResponseEntity<?> getQuestions(@PathVariable String categoryId) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", assessmentService.getQuestionsForCategory(categoryId)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/questions")
    public ResponseEntity<?> createQuestion(@RequestBody McqQuestionRequest req) {
        try {
            return ResponseEntity.status(201).body(Map.of("success", true, "data", assessmentService.createQuestion(req)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/questions/{questionId}")
    public ResponseEntity<?> updateQuestion(@PathVariable Long questionId, @RequestBody McqQuestionRequest req) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", assessmentService.updateQuestion(questionId, req)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long questionId) {
        try {
            assessmentService.deleteQuestion(questionId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Question deleted"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── Attempts ─────────────────────────────────────────────────────────────

    @GetMapping("/attempts")
    public ResponseEntity<?> getUserAttempts(Authentication auth) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", assessmentService.getUserAttempts(auth.getName())));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/attempts/{moduleId}")
    public ResponseEntity<?> submitAttempt(
            @PathVariable String moduleId,
            @RequestBody UserAttemptRequest req,
            Authentication auth) {
        try {
            return ResponseEntity.status(201).body(Map.of("success", true, "data",
                    assessmentService.submitAttempt(auth.getName(), moduleId, req)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
