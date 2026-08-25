package com.webapp.cognitodemo.controler;

import com.webapp.cognitodemo.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/*
 * NOTE: this endpoint currently relies on the same session/auth as the rest
 * of the API (ProtectedRoute on the frontend). There's no admin-only role
 * check yet - if/when a role field is added to User, this controller should
 * be locked down to that role rather than any authenticated user.
 */
@Tag(name = "Admin Dashboard", description = "Aggregate institutional stats for the admin panel")
@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    @Autowired
    private AdminDashboardService adminDashboardService;

    @Operation(summary = "Get aggregate dashboard stats (students, courses, enrollment, assessments)")
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", adminDashboardService.getDashboard()
        ));
    }

    @Operation(summary = "List all students with their enrollment/completion counts")
    @GetMapping("/students")
    public ResponseEntity<?> getStudents() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", adminDashboardService.getStudents()
        ));
    }
}
