package com.webapp.cognitodemo.service;

import com.webapp.cognitodemo.entity.User;
import com.webapp.cognitodemo.entity.support.SupportTicket;
import com.webapp.cognitodemo.repo.SupportTicketRepo;
import com.webapp.cognitodemo.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SupportTicketService {

    private static final int MAX_SCREENSHOTS = 5;
    private static final long MAX_SCREENSHOT_BYTES = 10L * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_TYPES = List.of("image/png", "image/jpeg", "image/jpg");

    @Autowired private SupportTicketRepo ticketRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private S3Service s3Service;

    private String currentAdminEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Not authenticated");
        }
        return auth.getName();
    }

    public Map<String, Object> submitTicket(String priority, String description, List<MultipartFile> screenshots) {
        String email = currentAdminEmail();
        String name = userRepo.findByEmail(email).map(User::getFullName).orElse(null);

        if (priority == null || priority.isBlank()) {
            throw new RuntimeException("Priority is required");
        }
        if (description == null || description.isBlank()) {
            throw new RuntimeException("Description is required");
        }
        if (description.length() > 1000) {
            throw new RuntimeException("Description must be 1000 characters or fewer");
        }

        List<MultipartFile> files = screenshots == null ? List.of() : screenshots.stream()
                .filter(f -> f != null && !f.isEmpty())
                .collect(Collectors.toList());
        if (files.size() > MAX_SCREENSHOTS) {
            throw new RuntimeException("You can attach at most " + MAX_SCREENSHOTS + " screenshots");
        }
        for (MultipartFile f : files) {
            if (f.getSize() > MAX_SCREENSHOT_BYTES) {
                throw new RuntimeException("Each screenshot must be 10MB or smaller");
            }
            if (f.getContentType() == null || !ALLOWED_TYPES.contains(f.getContentType().toLowerCase())) {
                throw new RuntimeException("Screenshots must be PNG or JPG/JPEG images");
            }
        }

        SupportTicket ticket = SupportTicket.builder()
                .submittedByEmail(email)
                .submittedByName(name)
                .priority(priority.toUpperCase())
                .description(description)
                .status("OPEN")
                .screenshotKeys(new ArrayList<>())
                // ticket_number is NOT NULL + unique at the DB level, but the real
                // "IS-0001" style code can only be derived from the id, which we
                // don't have until after the first insert. Use a unique placeholder
                // so this initial save doesn't violate the not-null/unique
                // constraint, then overwrite it with the real code below.
                .ticketNumber("TEMP-" + UUID.randomUUID())
                .build();
        ticket = ticketRepo.save(ticket);
        ticket.setTicketNumber(String.format("IS-%04d", ticket.getId()));

        List<String> keys = new ArrayList<>();
        try {
            int i = 1;
            for (MultipartFile f : files) {
                String original = f.getOriginalFilename();
                String ext = (original != null && original.contains("."))
                        ? original.substring(original.lastIndexOf('.')).toLowerCase()
                        : "";
                String key = "SupportTickets/" + email + "/" + ticket.getId() + "/screenshot-" + i + ext;
                keys.add(s3Service.upload(key, f));
                i++;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload screenshot(s)", e);
        }
        ticket.setScreenshotKeys(keys);
        ticket = ticketRepo.save(ticket);

        return toResponse(ticket);
    }

    public List<Map<String, Object>> getMyTickets() {
        String email = currentAdminEmail();
        return ticketRepo.findBySubmittedByEmailOrderByCreatedAtDesc(email).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toResponse(SupportTicket t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("ticketNumber", t.getTicketNumber());
        m.put("priority", t.getPriority());
        m.put("description", t.getDescription());
        m.put("status", t.getStatus());
        m.put("createdAt", t.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm")));
        List<String> screenshotUrls = (t.getScreenshotKeys() == null ? List.<String>of() : t.getScreenshotKeys())
                .stream()
                .map(key -> s3Service.presignedUrl(key, Duration.ofMinutes(30)))
                .collect(Collectors.toList());
        m.put("screenshotUrls", screenshotUrls);
        return m;
    }
}