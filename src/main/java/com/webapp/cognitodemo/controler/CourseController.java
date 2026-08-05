package com.webapp.cognitodemo.controler;

import com.webapp.cognitodemo.entity.course.CourseRequest;
import com.webapp.cognitodemo.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

import java.util.Map;

@Tag(name = "Courses", description = "Course catalogue")
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Operation(summary = "Get course types and all courses")
    @GetMapping
    public ResponseEntity<?> getCatalog() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", courseService.getCatalog()
        ));
    }

    @Operation(summary = "Create a new course")
    @PostMapping
    public ResponseEntity<?> createCourse(@Valid @RequestBody CourseRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "data", courseService.createCourse(request)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Update an existing course by ID")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCourse(
            @PathVariable String id,
            @Valid @RequestBody CourseRequest request) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", courseService.updateCourse(id, request)
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Delete a course by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable String id) {
        try {
            courseService.deleteCourse(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Course '" + id + "' deleted successfully"
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Get reviews for a course")
    @GetMapping("/{id}/reviews")
    public ResponseEntity<?> getReviews(@PathVariable String id) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", courseService.getReviewsForCourse(id)
        ));
    }
}
