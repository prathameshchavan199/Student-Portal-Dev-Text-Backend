package com.webapp.cognitodemo.entity.peer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class PeerProfileRequest {

    @NotBlank(message = "displayName is required")
    private String displayName;

    private String bio;
    private List<String> subjects;
    private List<String> skills;
    private String availability;
    private String experience;
    private String proficiencyLevel;
}
