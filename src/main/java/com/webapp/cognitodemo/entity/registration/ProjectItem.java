package com.webapp.cognitodemo.entity.registration;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectItem {
    private String projectType;
    private Boolean isTechnical;
    private String collegeName;
    private String role;
    private String duration;
    private String title;
    private String techStack;
    private String frontEnd;
    private String backEnd;
    private String database;
    private String description;
}
