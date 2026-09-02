package com.webapp.cognitodemo.entity.peer;

import com.webapp.cognitodemo.converter.StringListConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/*
 * A student's "teaching profile" — created once via the Peer to Peer
 * teaching flow ("Create Profile" step) before they can publish topics.
 * One profile per student email.
 */
@Entity
@Table(name = "peer_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String displayName;

    @Column(length = 1000)
    private String bio;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> subjects;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> skills;

    /* Free-text, e.g. "Mon - Fri, 4:00 PM - 8:00 PM" — matches the UI's simple range picker. */
    private String availability;

    /* Free-text, e.g. "2+ Years" */
    private String experience;

    /* Beginner / Intermediate / Advanced / Expert */
    private String proficiencyLevel;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
