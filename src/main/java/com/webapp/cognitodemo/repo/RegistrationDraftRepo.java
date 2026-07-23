package com.webapp.cognitodemo.repo;

import com.webapp.cognitodemo.entity.registration.StudentRegistrationDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegistrationDraftRepo extends JpaRepository<StudentRegistrationDraft, Long> {

    Optional<StudentRegistrationDraft> findByEmail(String email);
}
