package com.webapp.cognitodemo.entity.assessment;

import lombok.Data;

@Data
public class SpeakingSubmitRequest {
    private Long topicId;
    private String transcript;
    private int elapsedSecs;
}
