


package com.webapp.cognitodemo.controler;

import com.webapp.cognitodemo.service.TpoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

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

    @Operation(summary = " Assessment list — one row per assessment category (Technical Skills, Problem Solving, Communication, Data Skills), with undergraduate degree, students, and average score")
    @GetMapping("/assessments")
    public ResponseEntity<?> getAssessments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String degree,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", tpoService.getAssessments(search, degree, page, size)
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

    // ── Peer-to-Peer session requests (TPO approval gate) ────────────────────

    @Operation(summary = "Session Requests tab — peer-to-peer session requests from this college awaiting TPO approval")
    @GetMapping("/peer-sessions/pending")
    public ResponseEntity<?> getPendingPeerSessionRequests() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", tpoService.getPendingPeerSessionRequests()
        ));
    }

    @Operation(summary = "Teacher Student Requests tab — peer-to-peer sessions this TPO has already approved")
    @GetMapping("/peer-sessions/approved")
    public ResponseEntity<?> getApprovedPeerSessionRequests() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", tpoService.getApprovedPeerSessionRequests()
        ));
    }

    @Operation(summary = "Peer-to-peer session requests this TPO has rejected")
    @GetMapping("/peer-sessions/rejected")
    public ResponseEntity<?> getRejectedPeerSessionRequests() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", tpoService.getRejectedPeerSessionRequests()
        ));
    }

    @Operation(summary = "Approve or reject a pending peer-to-peer session request")
    @PostMapping("/peer-sessions/{id}/respond")
    public ResponseEntity<?> respondToPeerSessionRequest(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        try {
            Boolean approve = body.get("approve");
            if (approve == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "approve (true/false) is required"
                ));
            }
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", tpoService.respondToPeerSessionRequest(id, approve)
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}







//package com.webapp.cognitodemo.controler;
//
//import com.webapp.cognitodemo.service.TpoService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//import java.util.NoSuchElementException;
//
///*
// * TPO (Training & Placement Officer) admin panel endpoints.
// * Every route here is gated to ROLE_TPO_ADMIN in SecurityConfig.
// */
//@Tag(name = "TPO Admin", description = "Institutional dashboard, courses, and students for TPO admins")
//@RestController
//@RequestMapping("/api/tpo")
//public class TpoController {
//
//    @Autowired private TpoService tpoService;
//
//    @Operation(summary = "Institutional dashboard — course status, assessment status, undergraduate degree readiness")
//    @GetMapping("/dashboard")
//    public ResponseEntity<?> getDashboard() {
//        return ResponseEntity.ok(Map.of(
//                "success", true,
//                "data", tpoService.getDashboard()
//        ));
//    }
//
//    @Operation(summary = "Paginated course list with per-course completion status")
//    @GetMapping("/courses")
//    public ResponseEntity<?> getCourses(
//            @RequestParam(required = false) String search,
//            @RequestParam(required = false) String category,
//            @RequestParam(required = false) String degree,
//            @RequestParam(required = false) String status,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        return ResponseEntity.ok(Map.of(
//                "success", true,
//                "data", tpoService.getCourses(search, category, degree, status, page, size)
//        ));
//    }
//
//    @Operation(summary = " Assessment list — one row per assessment category (Technical Skills, Problem Solving, Communication, Data Skills), with undergraduate degree, students, and average score")
//    @GetMapping("/assessments")
//    public ResponseEntity<?> getAssessments(
//            @RequestParam(required = false) String search,
//            @RequestParam(required = false) String degree,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        return ResponseEntity.ok(Map.of(
//                "success", true,
//                "data", tpoService.getAssessments(search, degree, page, size)
//        ));
//    }
//
//    @Operation(summary = "Paginated student list with undergraduate degree, readiness, and completion")
//    @GetMapping("/students")
//    public ResponseEntity<?> getStudents(
//            @RequestParam(required = false) String search,
//            @RequestParam(required = false) String degree,
//            @RequestParam(required = false) String year,
//            @RequestParam(required = false) String status,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        return ResponseEntity.ok(Map.of(
//                "success", true,
//                "data", tpoService.getStudents(search, degree, year, status, page, size)
//        ));
//    }
//
//    // ── Peer-to-Peer session requests (TPO approval gate) ────────────────────
//
//    @Operation(summary = "Session Requests tab — peer-to-peer session requests from this college awaiting TPO approval")
//    @GetMapping("/peer-sessions/pending")
//    public ResponseEntity<?> getPendingPeerSessionRequests() {
//        return ResponseEntity.ok(Map.of(
//                "success", true,
//                "data", tpoService.getPendingPeerSessionRequests()
//        ));
//    }
//
//    @Operation(summary = "Teacher Student Requests tab — peer-to-peer sessions this TPO has already approved")
//    @GetMapping("/peer-sessions/approved")
//    public ResponseEntity<?> getApprovedPeerSessionRequests() {
//        return ResponseEntity.ok(Map.of(
//                "success", true,
//                "data", tpoService.getApprovedPeerSessionRequests()
//        ));
//    }
//
//    @Operation(summary = "Approve or reject a pending peer-to-peer session request")
//    @PostMapping("/peer-sessions/{id}/respond")
//    public ResponseEntity<?> respondToPeerSessionRequest(
//            @PathVariable Long id,
//            @RequestBody Map<String, Boolean> body) {
//        try {
//            Boolean approve = body.get("approve");
//            if (approve == null) {
//                return ResponseEntity.badRequest().body(Map.of(
//                        "success", false,
//                        "message", "approve (true/false) is required"
//                ));
//            }
//            return ResponseEntity.ok(Map.of(
//                    "success", true,
//                    "data", tpoService.respondToPeerSessionRequest(id, approve)
//            ));
//        } catch (NoSuchElementException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
//                    "success", false,
//                    "message", e.getMessage()
//            ));
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(Map.of(
//                    "success", false,
//                    "message", e.getMessage()
//            ));
//        }
//    }
//}



