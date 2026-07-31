package com.webapp.cognitodemo.repo;

import com.webapp.cognitodemo.entity.assessment.UserAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAttemptRepo extends JpaRepository<UserAttempt, Long> {
    List<UserAttempt> findByUserEmail(String email);
    int countByUserEmailAndModuleId(String email, String moduleId);
}
