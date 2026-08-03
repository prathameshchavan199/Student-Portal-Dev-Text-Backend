package com.webapp.cognitodemo.repo;

import com.webapp.cognitodemo.entity.assessment.WritingAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WritingAttemptRepo extends JpaRepository<WritingAttempt, Long> {
    List<WritingAttempt> findByUserEmailOrderByAttemptedAtDesc(String email);
    long countByUserEmailAndModuleId(String email, String moduleId);
}
