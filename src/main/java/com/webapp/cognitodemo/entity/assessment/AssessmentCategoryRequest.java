package com.webapp.cognitodemo.entity.assessment;

import lombok.Data;

@Data
public class AssessmentCategoryRequest {
    private String id;
    private String title;
    private String topbarLabel;
    private String heading;
    private String accentWord;
    private String subtitle;
    private String badge;
    private String tip;
    private String tag;
    private String icon;
    private String testTitle;
    private int displayOrder;
}
