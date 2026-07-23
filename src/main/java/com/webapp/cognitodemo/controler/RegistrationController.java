package com.webapp.cognitodemo.controler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webapp.cognitodemo.entity.registration.*;
import com.webapp.cognitodemo.service.RegistrationService;
import com.webapp.cognitodemo.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.Duration;
import java.util.*;

@Tag(name = "Registration", description = "Submit and draft registration forms; file upload/download via S3")
@RestController
@RequestMapping("/api/registration")
@CrossOrigin(
        origins = {"http://localhost:5173",
                "https://*.amplifyapp.com"},
        allowCredentials = "true"
)
public class RegistrationController {

    @Autowired private RegistrationService registrationService;
    @Autowired private S3Service s3Service;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /*
     * SUBMIT FINAL REGISTRATION
     * Multipart: "data" part = JSON, remaining parts = PDF files.
     */
    @Operation(summary = "Submit final registration (multipart: 'data' JSON part + optional PDF/image parts)")
    @RequestBody(required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(implementation = RegistrationFormDoc.class)))
    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submit(
            Authentication authentication,
            @Parameter(hidden = true) @RequestPart("data") String dataJson,
            @Parameter(hidden = true) @RequestPart(value = "marksheetFile",               required = false) MultipartFile marksheetFile,
            @Parameter(hidden = true) @RequestPart(value = "intermediateMarksheetFile",   required = false) MultipartFile intermediateMarksheetFile,
            @Parameter(hidden = true) @RequestPart(value = "diplomaMarksheetFile",        required = false) MultipartFile diplomaMarksheetFile,
            @Parameter(hidden = true) @RequestPart(value = "undergraduateMarksheetFile",  required = false) MultipartFile undergraduateMarksheetFile,
            @Parameter(hidden = true) @RequestPart(value = "postGraduationMarksheetFile", required = false) MultipartFile postGraduationMarksheetFile,
            @Parameter(hidden = true) @RequestPart(value = "resumeFile",                  required = false) MultipartFile resumeFile,
            @Parameter(hidden = true) @RequestPart(value = "profileImage",                required = false) MultipartFile profileImage
    ) {
        try {
            String email = authentication.getName();
            RegistrationRequest req = MAPPER.readValue(dataJson, RegistrationRequest.class);

            StudentRegistration saved = registrationService.submitRegistration(
                    email, req,
                    marksheetFile, intermediateMarksheetFile, diplomaMarksheetFile,
                    undergraduateMarksheetFile, postGraduationMarksheetFile,
                    resumeFile, profileImage
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Registration submitted successfully",
                    "id", saved.getId()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Submission failed"
            ));
        }
    }

    /*
     * SAVE DRAFT
     * Same multipart structure as /submit but persists to the draft table.
     */
    @Operation(summary = "Save draft — same multipart structure as /submit; upserts the draft table")
    @RequestBody(required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(implementation = RegistrationFormDoc.class)))
    @PostMapping(value = "/draft", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveDraft(
            Authentication authentication,
            @Parameter(hidden = true) @RequestPart("data") String dataJson,
            @Parameter(hidden = true) @RequestPart(value = "marksheetFile",               required = false) MultipartFile marksheetFile,
            @Parameter(hidden = true) @RequestPart(value = "intermediateMarksheetFile",   required = false) MultipartFile intermediateMarksheetFile,
            @Parameter(hidden = true) @RequestPart(value = "diplomaMarksheetFile",        required = false) MultipartFile diplomaMarksheetFile,
            @Parameter(hidden = true) @RequestPart(value = "undergraduateMarksheetFile",  required = false) MultipartFile undergraduateMarksheetFile,
            @Parameter(hidden = true) @RequestPart(value = "postGraduationMarksheetFile", required = false) MultipartFile postGraduationMarksheetFile,
            @Parameter(hidden = true) @RequestPart(value = "resumeFile",                  required = false) MultipartFile resumeFile,
            @Parameter(hidden = true) @RequestPart(value = "profileImage",                required = false) MultipartFile profileImage
    ) {
        try {
            String email = authentication.getName();
            RegistrationRequest req = MAPPER.readValue(dataJson, RegistrationRequest.class);

            StudentRegistrationDraft draft = registrationService.saveDraft(
                    email, req,
                    marksheetFile, intermediateMarksheetFile, diplomaMarksheetFile,
                    undergraduateMarksheetFile, postGraduationMarksheetFile,
                    resumeFile, profileImage
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Draft saved successfully",
                    "id", draft.getId()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Draft save failed"
            ));
        }
    }

    /*
     * GET CURRENT USER'S SUBMITTED REGISTRATION
     */
    @Operation(summary = "Get current user's submitted registration")
    @GetMapping("/me")
    public ResponseEntity<?> getMyRegistration(Authentication authentication) {
        String email = authentication.getName();
        return registrationService.getRegistration(email)
                .<ResponseEntity<?>>map(reg -> ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", sanitize(reg)
                )))
                .orElseGet(() -> ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "No registration found"
                )));
    }

    /*
     * GET CURRENT USER'S DRAFT
     */
    @Operation(summary = "Get current user's draft")
    @GetMapping("/draft/me")
    public ResponseEntity<?> getMyDraft(Authentication authentication) {
        String email = authentication.getName();
        return registrationService.getDraft(email)
                .<ResponseEntity<?>>map(draft -> ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", sanitize(draft)
                )))
                .orElseGet(() -> ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "No draft found"
                )));
    }

    /*
     * GET ALL SUBMITTED REGISTRATIONS
     */
    @Operation(summary = "Get all submitted registrations (admin)")
    @GetMapping("/all")
    public ResponseEntity<?> getAllRegistrations() {
        List<Map<String, Object>> result = registrationService.getAllRegistrations()
                .stream()
                .map(this::sanitize)
                .toList();
        return ResponseEntity.ok(Map.of("success", true, "data", result, "count", result.size()));
    }

    /*
     * GET ALL DRAFTS
     */
    @Operation(summary = "Get all drafts (admin)")
    @GetMapping("/draft/all")
    public ResponseEntity<?> getAllDrafts() {
        List<Map<String, Object>> result = registrationService.getAllDrafts()
                .stream()
                .map(this::sanitize)
                .toList();
        return ResponseEntity.ok(Map.of("success", true, "data", result, "count", result.size()));
    }

    /*
     * SERVE A FILE FROM SUBMITTED REGISTRATION
     * Redirects to a 15-minute presigned S3 URL.
     * type: tenthCertificate | intermediateCertificate | diplomaCertificate |
     *       undergraduateCertificate | postGraduationCertificate | resume | profileImage
     */
    @Operation(summary = "Get presigned S3 download URL for a file in submitted registration — open the returned 'url' in a browser tab")
    @GetMapping("/file/{id}/{type}")
    public ResponseEntity<?> getFile(
            @PathVariable Long id,
            @PathVariable String type,
            Authentication authentication) {

        return registrationService.getRegistration(authentication.getName())
                .map(reg -> presignedUrlResponse(reg, type))
                .orElse(ResponseEntity.notFound().build());
    }

    /*
     * SERVE A FILE FROM DRAFT
     */
    @Operation(summary = "Get presigned S3 download URL for a file in draft — open the returned 'url' in a browser tab")
    @GetMapping("/draft/file/{id}/{type}")
    public ResponseEntity<?> getDraftFile(
            @PathVariable Long id,
            @PathVariable String type,
            Authentication authentication) {

        return registrationService.getDraft(authentication.getName())
                .map(draft -> presignedUrlResponse(draft, type))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private ResponseEntity<?> presignedUrlResponse(BaseRegistration reg, String type) {
        String key = switch (type) {
            case "tenthCertificate"          -> reg.getTenthCertificateKey();
            case "intermediateCertificate"   -> reg.getIntermediateCertificateKey();
            case "diplomaCertificate"        -> reg.getDiplomaCertificateKey();
            case "undergraduateCertificate"  -> reg.getUndergraduateCertificateKey();
            case "postGraduationCertificate" -> reg.getPostGraduationCertificateKey();
            case "resume"                    -> reg.getResumeKey();
            case "profileImage"              -> reg.getProfileImageKey();
            default -> null;
        };

        if (key == null || key.isBlank()) return ResponseEntity.notFound().build();

        String url = s3Service.presignedUrl(key, Duration.ofMinutes(15));
        return ResponseEntity.ok(Map.of("success", true, "url", url));
    }

    // Strip binary blobs from the JSON response; replace with boolean flags.
    private Map<String, Object> sanitize(BaseRegistration reg) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id",                          reg.getId());
        map.put("email",                       nvl(reg.getEmail()));
        map.put("fullName",                    nvl(reg.getFullName()));
        map.put("phone",                       nvl(reg.getPhone()));
        map.put("country",                     nvl(reg.getCountry()));
        map.put("address",                     nvl(reg.getAddress()));
        map.put("school",                      nvl(reg.getSchool()));
        map.put("tenthGpa",                    nvl(reg.getTenthGpa()));
        map.put("tenthYearOfPassing",          nvl(reg.getTenthYearOfPassing()));
        map.put("hasTenthCertificate",         hasKey(reg.getTenthCertificateKey()));
        map.put("tenthCertificateFileName",    nvl(reg.getTenthCertificateFileName()));
        map.put("qualificationAfter10th",      nvl(reg.getQualificationAfter10th()));
        map.put("stream",                      nvl(reg.getStream()));
        map.put("intermediateCollege",         nvl(reg.getIntermediateCollege()));
        map.put("intermediateGpa",             nvl(reg.getIntermediateGpa()));
        map.put("intermediateYearOfPassing",   nvl(reg.getIntermediateYearOfPassing()));
        map.put("hasIntermediateCertificate",  hasKey(reg.getIntermediateCertificateKey()));
        map.put("intermediateCertificateFileName", nvl(reg.getIntermediateCertificateFileName()));
        map.put("diplomaBranch",               nvl(reg.getDiplomaBranch()));
        map.put("diplomaCollege",              nvl(reg.getDiplomaCollege()));
        map.put("diplomaGpa",                  nvl(reg.getDiplomaGpa()));
        map.put("diplomaYearOfPassing",        nvl(reg.getDiplomaYearOfPassing()));
        map.put("hasDiplomaCertificate",       hasKey(reg.getDiplomaCertificateKey()));
        map.put("diplomaCertificateFileName",  nvl(reg.getDiplomaCertificateFileName()));
        map.put("hasUndergraduate",            reg.getHasUndergraduate() != null && reg.getHasUndergraduate());
        map.put("undergraduateDegree",         nvl(reg.getUndergraduateDegree()));
        map.put("undergraduateOtherDegree",    nvl(reg.getUndergraduateOtherDegree()));
        map.put("btechBranch",                 nvl(reg.getBtechBranch()));
        map.put("undergraduateUniversity",     nvl(reg.getUndergraduateUniversity()));
        map.put("hasUndergraduateCertificate", hasKey(reg.getUndergraduateCertificateKey()));
        map.put("undergraduateCertificateFileName", nvl(reg.getUndergraduateCertificateFileName()));
        map.put("hasPostGraduation",           reg.getHasPostGraduation() != null && reg.getHasPostGraduation());
        map.put("postGraduationDegree",        nvl(reg.getPostGraduationDegree()));
        map.put("postGraduationOtherDegree",   nvl(reg.getPostGraduationOtherDegree()));
        map.put("postGraduationUniversity",    nvl(reg.getPostGraduationUniversity()));
        map.put("hasPostGraduationCertificate",hasKey(reg.getPostGraduationCertificateKey()));
        map.put("postGraduationCertificateFileName", nvl(reg.getPostGraduationCertificateFileName()));
        map.put("hasProjects",                 reg.getHasProjects() != null && reg.getHasProjects());
        map.put("projects",                    reg.getProjects() != null ? reg.getProjects() : Collections.emptyList());
        map.put("hasWorkExperience",           reg.getHasWorkExperience() != null && reg.getHasWorkExperience());
        map.put("positions",                   reg.getPositions() != null ? reg.getPositions() : Collections.emptyList());
        map.put("wantsAiProfile",              reg.getWantsAiProfile() != null && reg.getWantsAiProfile());
        map.put("hasResume",                   hasKey(reg.getResumeKey()));
        map.put("resumeFileName",              nvl(reg.getResumeFileName()));
        map.put("hasProfileImage",             hasKey(reg.getProfileImageKey()));
        map.put("profileImageFileName",        nvl(reg.getProfileImageFileName()));
        map.put("createdAt",                   reg.getCreatedAt());
        map.put("updatedAt",                   reg.getUpdatedAt());
        return map;
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private boolean hasKey(String key) { return key != null && !key.isBlank(); }
}
