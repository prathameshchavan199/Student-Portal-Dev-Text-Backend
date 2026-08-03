package com.webapp.cognitodemo.repo;

import com.webapp.cognitodemo.entity.assessment.SpeakingAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpeakingAttemptRepo extends JpaRepository<SpeakingAttempt, Long> {
    List<SpeakingAttempt> findByUserEmailOrderByAttemptedAtDesc(String email);
    long countByUserEmailAndModuleId(String email, String moduleId);
}
