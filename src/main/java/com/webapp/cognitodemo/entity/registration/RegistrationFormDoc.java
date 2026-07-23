package com.webapp.cognitodemo.entity.registration;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Swagger-only DTO describing the multipart/form-data body for /submit and /draft.
 * Not used by Spring at runtime — exists purely so springdoc emits full field docs.
 *
 * The real API accepts:
 *   "data"  — a JSON string containing all RegistrationRequest fields
 *   <name>  — binary PDF/image parts for each certificate / resume / profile image
 */
@Schema(description = "Multipart registration form. The 'data' part is a JSON object; all other parts are binary files.")
public class RegistrationFormDoc {

    // ── JSON data part ─────────────────────────────────────────────────────────

    @Schema(required = true, implementation = RegistrationRequest.class,
            description = "All text / boolean / list fields as a JSON object")
    public RegistrationRequest data;

    // ── File parts ─────────────────────────────────────────────────────────────

    @Schema(type = "string", format = "binary", description = "10th / SSC marksheet (PDF)")
    public Object marksheetFile;

    @Schema(type = "string", format = "binary", description = "12th / Intermediate marksheet (PDF)")
    public Object intermediateMarksheetFile;

    @Schema(type = "string", format = "binary", description = "Diploma marksheet (PDF)")
    public Object diplomaMarksheetFile;

    @Schema(type = "string", format = "binary", description = "Undergraduate degree marksheet (PDF)")
    public Object undergraduateMarksheetFile;

    @Schema(type = "string", format = "binary", description = "Post-graduation marksheet (PDF)")
    public Object postGraduationMarksheetFile;

    @Schema(type = "string", format = "binary", description = "Resume (PDF)")
    public Object resumeFile;

    @Schema(type = "string", format = "binary", description = "Profile image (JPG / PNG)")
    public Object profileImage;
}
