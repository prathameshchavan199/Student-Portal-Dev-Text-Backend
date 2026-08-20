package com.webapp.cognitodemo.controler;

import com.webapp.cognitodemo.service.CourseProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

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

    @GetMapping("/{courseId}")
    public ResponseEntity<?> getCourseProgress(
            @PathVariable String courseId,
            Authentication auth) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data",    progressService.getCourseProgressDetail(auth.getName(), courseId)
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
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

    /*
     * Marks lesson "{moduleIndex}-{lessonIndex}" (e.g. "0-1") as watched.
     * Recalculates overall progressPct and auto-promotes the course status.
     */
    @PostMapping("/{courseId}/lesson/{lessonKey}/complete")
    public ResponseEntity<?> completeLesson(
            @PathVariable String courseId,
            @PathVariable String lessonKey,
            Authentication auth) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data",    progressService.markLessonComplete(auth.getName(), courseId, lessonKey)
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /* Records which lesson the learner currently has open (for "resume"). */
    @PostMapping("/{courseId}/lesson/{lessonKey}/open")
    public ResponseEntity<?> openLesson(
            @PathVariable String courseId,
            @PathVariable String lessonKey,
            Authentication auth) {
        progressService.setLastLesson(auth.getName(), courseId, lessonKey);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
