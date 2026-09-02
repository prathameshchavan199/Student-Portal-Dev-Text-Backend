package com.webapp.cognitodemo.entity.peer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class PeerTopicRequest {

    @NotBlank(message = "title is required")
    private String title;

    private String description;
    private String level;
    private List<String> tags;
    private List<String> availableDays;
    private String timeSlotStart;
    private String timeSlotEnd;
    private String sessionDuration;
}
