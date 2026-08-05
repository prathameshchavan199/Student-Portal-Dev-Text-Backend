package com.webapp.cognitodemo.entity.course;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CourseReviewRequest {

    @NotBlank(message = "reviewerName is required")
    private String reviewerName;

    @NotNull(message = "rating is required")
    @Min(value = 1, message = "rating must be between 1 and 5")
    @Max(value = 5, message = "rating must be between 1 and 5")
    private Integer rating;

    @NotBlank(message = "reviewText is required")
    private String reviewText;
}
