package com.webapp.cognitodemo.repo;

import com.webapp.cognitodemo.entity.assessment.AssessmentModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentModuleRepo extends JpaRepository<AssessmentModule, String> {
    List<AssessmentModule> findByCategoryIdOrderByDisplayOrder(String categoryId);
}
