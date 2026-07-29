package com.webapp.cognitodemo.entity.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CourseRequest {

    @NotBlank(message = "id is required (slug like 'react-essentials')")
    private String id;

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "category must be onDemand, onlineProgram, or offlineProgram")
    private String category;

    @NotNull(message = "price is required")
    private Integer price;

    private String duration;
    private String level;
    private String imageUrl;
    private String instructor;
    private String description;
    private String courseArea;
    private String topic;
    private String format;
    private String date;
    private String time;
    private String platform;
    private String location;
    private String startsIn;
    private Integer seatsLeft;
    private String accent;

    // Each session: { "id": "...", "title": "...", "date": "...", "time": "..." }
    private List<Map<String, String>> sessions;
}
