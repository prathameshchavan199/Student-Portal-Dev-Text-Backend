package com.webapp.cognitodemo.service;

import com.webapp.cognitodemo.entity.registration.*;
import com.webapp.cognitodemo.repo.RegistrationDraftRepo;
import com.webapp.cognitodemo.repo.RegistrationRepo;
import com.webapp.cognitodemo.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RegistrationService {

    @Autowired private RegistrationRepo registrationRepo;
    @Autowired private RegistrationDraftRepo draftRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private S3Service s3Service;

    @Transactional
    public StudentRegistration submitRegistration(
            String email,
            RegistrationRequest req,
            MultipartFile marksheetFile,
            MultipartFile intermediateMarksheetFile,
            MultipartFile diplomaMarksheetFile,
            MultipartFile undergraduateMarksheetFile,
            MultipartFile postGraduationMarksheetFile,
            MultipartFile resumeFile,
            MultipartFile profileImage) throws IOException {

        StudentRegistration reg = registrationRepo.findByEmail(email)
                .orElse(new StudentRegistration());

        applyFields(reg, email, req, marksheetFile, intermediateMarksheetFile,
                diplomaMarksheetFile, undergraduateMarksheetFile,
                postGraduationMarksheetFile, resumeFile, profileImage);

        reg.setSubmittedAt(LocalDateTime.now());
        StudentRegistration saved = registrationRepo.save(reg);

        userRepo.findByEmail(email).ifPresent(user -> {
            user.setRegistration(true);
            userRepo.save(user);
        });

        return saved;
    }

    @Transactional
    public StudentRegistrationDraft saveDraft(
            String email,
            RegistrationRequest req,
            MultipartFile marksheetFile,
            MultipartFile intermediateMarksheetFile,
            MultipartFile diplomaMarksheetFile,
            MultipartFile undergraduateMarksheetFile,
            MultipartFile postGraduationMarksheetFile,
            MultipartFile resumeFile,
            MultipartFile profileImage) throws IOException {

        StudentRegistrationDraft draft = draftRepo.findByEmail(email)
                .orElse(new StudentRegistrationDraft());

        applyFields(draft, email, req, marksheetFile, intermediateMarksheetFile,
                diplomaMarksheetFile, undergraduateMarksheetFile,
                postGraduationMarksheetFile, resumeFile, profileImage);

        draft.setLastSavedAt(LocalDateTime.now());
        return draftRepo.save(draft);
    }

    public Optional<StudentRegistration> getRegistration(String email) {
        return registrationRepo.findByEmail(email);
    }

    public Optional<StudentRegistrationDraft> getDraft(String email) {
        return draftRepo.findByEmail(email);
    }

    public List<StudentRegistration> getAllRegistrations() {
        return registrationRepo.findAll();
    }

    public List<StudentRegistrationDraft> getAllDrafts() {
        return draftRepo.findAll();
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private void applyFields(
            BaseRegistration target,
            String email,
            RegistrationRequest req,
            MultipartFile marksheetFile,
            MultipartFile intermediateMarksheetFile,
            MultipartFile diplomaMarksheetFile,
            MultipartFile undergraduateMarksheetFile,
            MultipartFile postGraduationMarksheetFile,
            MultipartFile resumeFile,
            MultipartFile profileImage) throws IOException {

        target.setEmail(email);

        // Personal info
        target.setFullName(req.getFullName());
        target.setPhone(req.getPhone());
        target.setCountry(req.getCountry());
        target.setAddress(req.getAddress());

        // 10th
        target.setSchool(req.getSchool());
        target.setTenthGpa(req.getGpa());
        target.setTenthYearOfPassing(req.getYearOfPassing());
        if (hasContent(marksheetFile)) {
            String key = s3Service.upload(s3Key(email, "tenth_certificate", marksheetFile), marksheetFile);
            target.setTenthCertificateKey(key);
            target.setTenthCertificateFileName(marksheetFile.getOriginalFilename());
        }

        // After 10th
        target.setQualificationAfter10th(req.getQualificationAfter10th());

        // Intermediate
        target.setStream(req.getStream());
        target.setIntermediateCollege(req.getGratudatecollege());
        target.setIntermediateGpa(req.getIntermediateGpa());
        target.setIntermediateYearOfPassing(req.getIntermediateYearOfPassing());
        if (hasContent(intermediateMarksheetFile)) {
            String key = s3Service.upload(s3Key(email, "intermediate_certificate", intermediateMarksheetFile), intermediateMarksheetFile);
            target.setIntermediateCertificateKey(key);
            target.setIntermediateCertificateFileName(intermediateMarksheetFile.getOriginalFilename());
        }

        // Diploma
        target.setDiplomaBranch(req.getDiplomaBranch());
        target.setDiplomaCollege(req.getDiplomacollege());
        target.setDiplomaGpa(req.getDiplomaGpa());
        target.setDiplomaYearOfPassing(req.getDiplomaYearOfPassing());
        if (hasContent(diplomaMarksheetFile)) {
            String key = s3Service.upload(s3Key(email, "diploma_certificate", diplomaMarksheetFile), diplomaMarksheetFile);
            target.setDiplomaCertificateKey(key);
            target.setDiplomaCertificateFileName(diplomaMarksheetFile.getOriginalFilename());
        }

        // Undergraduate
        target.setHasUndergraduate(req.getHasUndergraduate());
        target.setUndergraduateDegree(req.getUndergraduateDegree());
        target.setUndergraduateOtherDegree(req.getUndergraduateOtherDegree());
        target.setBtechBranch(req.getBtechDegree());
        target.setUndergraduateUniversity(req.getUndergraduateUniversity());
        if (hasContent(undergraduateMarksheetFile)) {
            String key = s3Service.upload(s3Key(email, "undergraduate_certificate", undergraduateMarksheetFile), undergraduateMarksheetFile);
            target.setUndergraduateCertificateKey(key);
            target.setUndergraduateCertificateFileName(undergraduateMarksheetFile.getOriginalFilename());
        }

        // Postgraduate
        target.setHasPostGraduation(req.getHasPostGraduation());
        target.setPostGraduationDegree(req.getPostGraduationDegree());
        target.setPostGraduationOtherDegree(req.getPostGraduationOtherDegree());
        target.setPostGraduationUniversity(req.getPostGraduationUniversity());
        if (hasContent(postGraduationMarksheetFile)) {
            String key = s3Service.upload(s3Key(email, "post_graduation_certificate", postGraduationMarksheetFile), postGraduationMarksheetFile);
            target.setPostGraduationCertificateKey(key);
            target.setPostGraduationCertificateFileName(postGraduationMarksheetFile.getOriginalFilename());
        }

        // Projects
        target.setHasProjects(req.getHasProjects());
        target.setProjects(req.getProjects());

        // Work Experience
        target.setHasWorkExperience(req.getHasWorkExperience());
        target.setPositions(req.getPositions());

        // Profile / Resume
        target.setWantsAiProfile(req.getWantsAiProfile());
        if (hasContent(resumeFile)) {
            String key = s3Service.upload(s3Key(email, "resume", resumeFile), resumeFile);
            target.setResumeKey(key);
            target.setResumeFileName(resumeFile.getOriginalFilename());
        }
        if (hasContent(profileImage)) {
            String key = s3Service.upload(s3Key(email, "profile_image", profileImage), profileImage);
            target.setProfileImageKey(key);
            target.setProfileImageFileName(profileImage.getOriginalFilename());
        }
    }

    /*
     * Builds the S3 key: UserDetails/{email}/{type}.{ext}
     * Fixed name per type so re-uploads overwrite the old file naturally.
     */
    private String s3Key(String email, String type, MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? "." + original.substring(original.lastIndexOf('.') + 1).toLowerCase()
                : "";
        return "UserDetails/" + email + "/" + type + ext;
    }

    private boolean hasContent(MultipartFile file) {
        return file != null && !file.isEmpty();
    }
}
