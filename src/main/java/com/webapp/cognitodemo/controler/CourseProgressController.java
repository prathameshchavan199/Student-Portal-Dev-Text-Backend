package com.webapp.cognitodemo.controler;

import com.webapp.cognitodemo.service.CourseProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/course-progress")
public class CourseProgressController {

    @Autowired private CourseProgressService progressService;

    @GetMapping
    public ResponseEntity<?> getMyProgress(Authentication auth) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data",    progressService.getUserProgress(auth.getName())
        ));
    }

    @PostMapping("/{courseId}/start")
    public ResponseEntity<?> startCourse(
            @PathVariable String courseId,
            Authentication auth) {
        progressService.startCourse(auth.getName(), courseId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{courseId}/complete")
    public ResponseEntity<?> completeCourse(
            @PathVariable String courseId,
            Authentication auth) {
        progressService.completeCourse(auth.getName(), courseId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{courseId}/certify")
    public ResponseEntity<?> certifyCourse(
            @PathVariable String courseId,
            Authentication auth) {
        progressService.certifyCourse(auth.getName(), courseId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
