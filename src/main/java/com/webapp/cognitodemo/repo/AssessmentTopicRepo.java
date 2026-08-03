package com.webapp.cognitodemo.repo;

import com.webapp.cognitodemo.entity.assessment.AssessmentTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentTopicRepo extends JpaRepository<AssessmentTopic, Long> {
    List<AssessmentTopic> findByTypeAndActiveTrue(String type);
}
