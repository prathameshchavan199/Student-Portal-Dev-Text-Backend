package com.webapp.cognitodemo.controler;

import com.webapp.cognitodemo.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/*
 * TPO admin "Support" page — raising and viewing issues submitted to the
 * platform team. Gated to ROLE_TPO_ADMIN (same as /api/tpo/**, path just
 * lives under its own namespace for clarity).
 */
@Tag(name = "TPO Admin", description = "Institutional dashboard, courses, and students for TPO admins")
@RestController
@RequestMapping("/api/tpo/support")
public class SupportTicketController {

    @Autowired private SupportTicketService supportTicketService;

    @Operation(summary = "List the current TPO admin's previously submitted support tickets")
    @GetMapping("/tickets")
    public ResponseEntity<?> getMyTickets() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", supportTicketService.getMyTickets()
        ));
    }

    @Operation(summary = "Submit a new support ticket, optionally with up to 5 screenshots")
    @PostMapping(value = "/tickets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submitTicket(
            @RequestParam String priority,
            @RequestParam String description,
            @Parameter(hidden = true) @RequestPart(value = "screenshots", required = false) List<MultipartFile> screenshots) {
        try {
            Map<String, Object> ticket = supportTicketService.submitTicket(priority, description, screenshots);
            return ResponseEntity.ok(Map.of("success", true, "data", ticket));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
