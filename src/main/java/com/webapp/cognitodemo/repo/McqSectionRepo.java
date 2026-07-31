package com.webapp.cognitodemo.repo;

import com.webapp.cognitodemo.entity.assessment.McqSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface McqSectionRepo extends JpaRepository<McqSection, Long> {
    List<McqSection> findByCategoryIdOrderBySectionOrder(String categoryId);
}
