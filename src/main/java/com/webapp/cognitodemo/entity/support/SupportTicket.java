package com.webapp.cognitodemo.entity.support;

import com.webapp.cognitodemo.converter.StringListConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/*
 * A support ticket raised by a TPO admin from the TPO Support page.
 * Tickets are scoped to the admin who raised them (submittedByEmail) — the
 * TPO panel only shows an admin their own submitted issues, not other
 * admins' tickets.
 */
@Entity
@Table(name = "support_tickets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* Display code, e.g. "IS-0001" — assigned right after the row gets its id. */
    @Column(nullable = false, unique = true)
    private String ticketNumber;

    @Column(nullable = false)
    private String submittedByEmail;

    private String submittedByName;

    /* LOW / MEDIUM / HIGH */
    @Column(nullable = false)
    private String priority;

    @Column(nullable = false, length = 1000)
    private String description;

    /* OPEN / IN_PROGRESS / RESOLVED — starts OPEN; changed by whoever handles
     * tickets on the backend, there's no self-service status change here. */
    @Column(nullable = false)
    private String status;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> screenshotKeys;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null || status.isBlank()) status = "OPEN";
    }
}
