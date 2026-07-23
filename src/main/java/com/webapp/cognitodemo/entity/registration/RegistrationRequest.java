package com.webapp.cognitodemo.entity.registration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "All non-file registration fields (sent as JSON in the 'data' multipart part)")
public class RegistrationRequest {

    // ── Personal Info ─────────────────────────────────────────────────────────
    @Schema(description = "Student full name", example = "John Doe")
    private String fullName;
    @Schema(description = "Student email", example = "john@example.com")
    private String email;
    @Schema(description = "Mobile number", example = "+91 9876543210")
    private String phone;
    @Schema(example = "India")
    private String country;
    @Schema(example = "123 Main St, Pune")
    private String address;

    // ── 10th Education ────────────────────────────────────────────────────────
    @Schema(description = "School name", example = "Delhi Public School")
    private String school;
    @Schema(description = "10th GPA / percentage", example = "9.2")
    private String gpa;
    @Schema(description = "10th year of passing", example = "2018")
    private String yearOfPassing;

    // ── After-10th qualification: 'intermediate' or 'diploma' ─────────────────
    @Schema(description = "'intermediate' or 'diploma'", example = "intermediate")
    private String qualificationAfter10th;

    // ── Intermediate / 12th ───────────────────────────────────────────────────
    @Schema(example = "Science")
    private String stream;
    @Schema(description = "Intermediate college name", example = "St. Xavier's College")
    private String gratudatecollege;
    @Schema(example = "88.5")
    private String intermediateGpa;
    @Schema(example = "2020")
    private String intermediateYearOfPassing;

    // ── Diploma ───────────────────────────────────────────────────────────────
    @Schema(example = "Computer Engineering")
    private String diplomaBranch;
    @Schema(example = "Government Polytechnic Pune")
    private String diplomacollege;
    @Schema(example = "7.8")
    private String diplomaGpa;
    @Schema(example = "2020")
    private String diplomaYearOfPassing;

    // ── Undergraduate ─────────────────────────────────────────────────────────
    @Schema(description = "Whether the student has completed undergraduate education")
    private Boolean hasUndergraduate;
    @Schema(description = "Degree name, e.g. 'B.Tech', 'BCA', 'Other'", example = "B.Tech")
    private String undergraduateDegree;
    @Schema(description = "Filled when undergraduateDegree = 'Other'", example = "B.Voc")
    private String undergraduateOtherDegree;
    @Schema(description = "B.Tech branch (filled when undergraduateDegree = 'B.Tech')", example = "Computer Science")
    private String btechDegree;
    @Schema(example = "Pune University")
    private String undergraduateUniversity;

    // ── Postgraduate ──────────────────────────────────────────────────────────
    @Schema(description = "Whether the student has completed post-graduation")
    private Boolean hasPostGraduation;
    @Schema(description = "PG degree name, e.g. 'M.Tech', 'MBA', 'Other'", example = "M.Tech")
    private String postGraduationDegree;
    @Schema(description = "Filled when postGraduationDegree = 'Other'", example = "M.Voc")
    private String postGraduationOtherDegree;
    @Schema(example = "IIT Bombay")
    private String postGraduationUniversity;

    // ── Projects ──────────────────────────────────────────────────────────────
    @Schema(description = "Whether the student has projects to list")
    private Boolean hasProjects;
    private List<ProjectItem> projects;

    // ── Work Experience ───────────────────────────────────────────────────────
    @Schema(description = "Whether the student has work experience")
    private Boolean hasWorkExperience;
    private List<PositionItem> positions;

    // ── Profile / Resume ──────────────────────────────────────────────────────
    @Schema(description = "Whether the student wants an AI-generated profile")
    private Boolean wantsAiProfile;
}
