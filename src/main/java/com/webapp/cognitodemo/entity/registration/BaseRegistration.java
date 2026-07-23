package com.webapp.cognitodemo.entity.registration;

import com.webapp.cognitodemo.converter.PositionListConverter;
import com.webapp.cognitodemo.converter.ProjectListConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    // ── Personal Information ────────────────────────────────────────────────
    private String fullName;
    private String phone;
    private String country;

    @Column(columnDefinition = "TEXT")
    private String address;

    // ── Secondary Education (10th) ──────────────────────────────────────────
    private String school;
    private String tenthGpa;
    private String tenthYearOfPassing;

    // S3 object key — e.g. UserDetails/user@email.com/tenth_certificate.pdf
    private String tenthCertificateKey;
    private String tenthCertificateFileName;

    // ── Qualification after 10th ────────────────────────────────────────────
    private String qualificationAfter10th;

    // Intermediate / 12th
    private String stream;
    private String intermediateCollege;
    private String intermediateGpa;
    private String intermediateYearOfPassing;

    private String intermediateCertificateKey;
    private String intermediateCertificateFileName;

    // Diploma
    private String diplomaBranch;
    private String diplomaCollege;
    private String diplomaGpa;
    private String diplomaYearOfPassing;

    private String diplomaCertificateKey;
    private String diplomaCertificateFileName;

    // ── Undergraduate ───────────────────────────────────────────────────────
    private Boolean hasUndergraduate;
    private String undergraduateDegree;
    private String undergraduateOtherDegree;
    private String btechBranch;
    private String undergraduateUniversity;

    private String undergraduateCertificateKey;
    private String undergraduateCertificateFileName;

    // ── Postgraduate ────────────────────────────────────────────────────────
    private Boolean hasPostGraduation;
    private String postGraduationDegree;
    private String postGraduationOtherDegree;
    private String postGraduationUniversity;

    private String postGraduationCertificateKey;
    private String postGraduationCertificateFileName;

    // ── Projects ────────────────────────────────────────────────────────────
    private Boolean hasProjects;

    @Convert(converter = ProjectListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<ProjectItem> projects;

    // ── Work Experience ─────────────────────────────────────────────────────
    private Boolean hasWorkExperience;

    @Convert(converter = PositionListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<PositionItem> positions;

    // ── Profile / Resume ────────────────────────────────────────────────────
    private Boolean wantsAiProfile;

    private String resumeKey;
    private String resumeFileName;

    private String profileImageKey;
    private String profileImageFileName;

    // ── Audit ───────────────────────────────────────────────────────────────
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
