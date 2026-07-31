package com.webapp.cognitodemo.entity.assessment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "assessment_modules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentModule {

    @Id
    private String id;

    @Column(nullable = false)
    private String categoryId;

    @Column(nullable = false)
    private String title;

    private String level;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String tag;
    private String type;
    private String icon;
    private String duration;
    private Integer questions;

    @Column(name = "display_order")
    private int displayOrder;
}
