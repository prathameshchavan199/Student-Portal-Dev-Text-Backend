package com.webapp.cognitodemo.repo;

import com.webapp.cognitodemo.entity.registration.StudentRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegistrationRepo extends JpaRepository<StudentRegistration, Long> {

    Optional<StudentRegistration> findByEmail(String email);

    boolean existsByEmail(String email);
}
