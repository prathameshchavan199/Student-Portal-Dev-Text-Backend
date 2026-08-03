package com.webapp.cognitodemo.entity.assessment;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "assessment_topics")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AssessmentTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type; // "speaking" | "writing"

    @Column(columnDefinition = "TEXT")
    private String topicText;

    @Column(columnDefinition = "TEXT")
    private String challenge;

    @Column(columnDefinition = "TEXT")
    private String keywordsJson;

    private boolean active;

    private int displayOrder;
}
