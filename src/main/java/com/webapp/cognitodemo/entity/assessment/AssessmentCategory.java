package com.webapp.cognitodemo.entity.assessment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "assessment_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentCategory {

    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    private String topbarLabel;
    private String heading;
    private String accentWord;

    @Column(columnDefinition = "TEXT")
    private String subtitle;

    private String badge;
    private String tip;
    private String tag;
    private String icon;
    private String testTitle;

    @Column(name = "display_order")
    private int displayOrder;
}
