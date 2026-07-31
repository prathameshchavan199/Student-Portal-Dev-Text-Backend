package com.webapp.cognitodemo.entity.assessment;

import lombok.Data;

@Data
public class AssessmentModuleRequest {
    private String id;
    private String categoryId;
    private String title;
    private String level;
    private String description;
    private String tag;
    private String type;
    private String icon;
    private String duration;
    private Integer questions;
    private int displayOrder;
}
