package com.webapp.cognitodemo.entity.registration;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "student_registration")
public class StudentRegistration extends BaseRegistration {

    private LocalDateTime submittedAt;
}
