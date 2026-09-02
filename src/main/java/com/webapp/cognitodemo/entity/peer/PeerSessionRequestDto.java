package com.webapp.cognitodemo.entity.peer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PeerSessionRequestDto {

    @NotNull(message = "topicId is required")
    private Long topicId;

    @NotBlank(message = "requestedDate is required")
    private String requestedDate;

    private String requestedTimeSlot;
    private String message;
}
