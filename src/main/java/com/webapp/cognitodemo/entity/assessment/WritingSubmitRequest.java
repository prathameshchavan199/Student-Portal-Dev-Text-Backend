package com.webapp.cognitodemo.entity.assessment;

import lombok.Data;

@Data
public class WritingSubmitRequest {
    private Long topicId;
    private String text;
}
