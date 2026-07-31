package com.webapp.cognitodemo.entity.assessment;

import lombok.Data;

@Data
public class McqQuestionRequest {
    private Long sectionId;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private int correctAnswer;
    private int questionOrder;
}
