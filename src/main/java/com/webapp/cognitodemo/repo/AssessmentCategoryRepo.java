package com.webapp.cognitodemo.repo;

import com.webapp.cognitodemo.entity.assessment.AssessmentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentCategoryRepo extends JpaRepository<AssessmentCategory, String> {
}
