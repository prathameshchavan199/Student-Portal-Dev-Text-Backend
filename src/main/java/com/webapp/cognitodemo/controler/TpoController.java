package com.webapp.cognitodemo.controler;

import com.webapp.cognitodemo.service.TpoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/*
 * TPO (Training & Placement Officer) admin panel endpoints.
 * Every route here is gated to ROLE_TPO_ADMIN in SecurityConfig.
 */
@Tag(name = "TPO Admin", description = "Institutional dashboard, courses, and students for TPO admins")
@RestController
@RequestMapping("/api/tpo")
public class TpoController {

    @Autowired private TpoService tpoService;

    @Operation(summary = "Institutional dashboard — course status, assessment status, undergraduate degree readiness")
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", tpoService.getDashboard()
        ));
    }

    @Operation(summary = "Paginated course list with per-course completion status")
    @GetMapping("/courses")
    public ResponseEntity<?> getCourses(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String degree,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", tpoService.getCourses(search, category, degree, status, page, size)
        ));
    }

    @Operation(summary = "Paginated assessment-module list with undergraduate degree, students, and average score")
    @GetMapping("/assessments")
    public ResponseEntity<?> getAssessments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String degree,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", tpoService.getAssessments(search, type, degree, page, size)
        ));
    }

    @Operation(summary = "Paginated student list with undergraduate degree, readiness, and completion")
    @GetMapping("/students")
    public ResponseEntity<?> getStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String degree,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", tpoService.getStudents(search, degree, year, status, page, size)
        ));
    }
}
