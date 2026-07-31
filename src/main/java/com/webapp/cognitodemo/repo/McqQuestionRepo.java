package com.webapp.cognitodemo.repo;

import com.webapp.cognitodemo.entity.assessment.McqQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface McqQuestionRepo extends JpaRepository<McqQuestion, Long> {
    List<McqQuestion> findBySectionIdOrderByQuestionOrder(Long sectionId);
}
