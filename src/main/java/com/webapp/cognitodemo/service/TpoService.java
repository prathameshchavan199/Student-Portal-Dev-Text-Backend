package com.webapp.cognitodemo.service;

import com.webapp.cognitodemo.entity.course.Course;
import com.webapp.cognitodemo.entity.course.CourseProgress;
import com.webapp.cognitodemo.entity.assessment.UserAttempt;
import com.webapp.cognitodemo.entity.registration.StudentRegistration;
import com.webapp.cognitodemo.repo.AssessmentModuleRepo;
import com.webapp.cognitodemo.repo.CourseProgressRepo;
import com.webapp.cognitodemo.repo.CourseRepo;
import com.webapp.cognitodemo.repo.RegistrationRepo;
import com.webapp.cognitodemo.repo.UserAttemptRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/*
 * Backs the TPO (Training & Placement Officer) admin panel.
 *
 * Every number here is derived from data that genuinely exists in this
 * system (courses, course-progress, student registrations, assessment
 * attempts) — there is no placement/campus-drive/offers data model yet,
 * so that section is intentionally left out rather than faked.
 */
@Service
public class TpoService {

    @Autowired private CourseRepo courseRepo;
    @Autowired private CourseProgressRepo progressRepo;
    @Autowired private RegistrationRepo registrationRepo;
    @Autowired private UserAttemptRepo attemptRepo;
    @Autowired private AssessmentModuleRepo moduleRepo;

    // ── Dashboard ────────────────────────────────────────────────────────────

    public Map<String, Object> getDashboard() {
        List<Course> courses = courseRepo.findAll();
        List<CourseProgress> allProgress = progressRepo.findAll();
        List<StudentRegistration> students = registrationRepo.findAll();
        List<UserAttempt> attempts = attemptRepo.findAll();
        long totalModules = moduleRepo.count();

        Map<String, List<CourseProgress>> progressByCourse = allProgress.stream()
                .collect(Collectors.groupingBy(CourseProgress::getCourseId));

        int coursesCompleted = 0, coursesInProgress = 0, coursesNotStarted = 0;
        for (Course c : courses) {
            String bucket = bucketForCourse(progressByCourse.getOrDefault(c.getId(), List.of()));
            switch (bucket) {
                case "Completed" -> coursesCompleted++;
                case "In Progress" -> coursesInProgress++;
                default -> coursesNotStarted++;
            }
        }

        Map<String, Object> courseStatus = new LinkedHashMap<>();
        courseStatus.put("total", courses.size());
        courseStatus.put("completed", coursesCompleted);
        courseStatus.put("inProgress", coursesInProgress);
        courseStatus.put("notStarted", coursesNotStarted);

        double avgAttemptScorePct = attempts.isEmpty() ? 0 : attempts.stream()
                .mapToDouble(a -> a.getTotal() > 0 ? (a.getScore() * 100.0 / a.getTotal()) : 0)
                .average().orElse(0);

        Map<String, Object> assessmentStatus = new LinkedHashMap<>();
        assessmentStatus.put("totalAttempts", attempts.size());
        assessmentStatus.put("totalModules", totalModules);
        assessmentStatus.put("averageScorePct", Math.round(avgAttemptScorePct * 10) / 10.0);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCourses", courses.size());
        stats.put("totalStudents", students.size());
        stats.put("totalEnrollments", allProgress.size());
        stats.put("courseStatus", courseStatus);
        stats.put("assessmentStatus", assessmentStatus);
        stats.put("departmentReadiness", departmentReadinessIndex(students, attempts));
        stats.put("studentQualification", studentQualification(students, attempts));
        stats.put("assessmentPillars", assessmentPillars(attempts));

        return stats;
    }

    /*
     * Buckets students by their average assessment score so far:
     * fully qualified (>=75%), in evaluation (some attempts, <75%),
     * remediation assigned (no attempts yet). Derived from real attempt data,
     * not a tracked qualification workflow (none exists in this system).
     */
    private Map<String, Object> studentQualification(List<StudentRegistration> students, List<UserAttempt> attempts) {
        Map<String, List<Double>> scoresByEmail = attempts.stream()
                .filter(a -> a.getTotal() > 0)
                .collect(Collectors.groupingBy(
                        UserAttempt::getUserEmail,
                        Collectors.mapping(a -> a.getScore() * 100.0 / a.getTotal(), Collectors.toList())
                ));

        int fullyQualified = 0, inEvaluation = 0, remediationAssigned = 0;
        for (StudentRegistration s : students) {
            List<Double> scores = scoresByEmail.getOrDefault(s.getEmail(), List.of());
            if (scores.isEmpty()) {
                remediationAssigned++;
                continue;
            }
            double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            if (avg >= 75) fullyQualified++;
            else inEvaluation++;
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalStudents", students.size());
        m.put("fullyQualified", fullyQualified);
        m.put("inEvaluation", inEvaluation);
        m.put("remediationAssigned", remediationAssigned);
        return m;
    }

    /*
     * Average score (%) grouped by assessment-module category (categoryId),
     * e.g. "Coding Sandboxes", "Aptitude & Logic" — whatever categories exist
     * in assessment_modules. Real attempt data, grouped by real category.
     */
    private List<Map<String, Object>> assessmentPillars(List<UserAttempt> attempts) {
        Map<String, String> categoryByModule = moduleRepo.findAll().stream()
                .collect(Collectors.toMap(
                        m -> m.getId(),
                        m -> m.getCategoryId() == null || m.getCategoryId().isBlank() ? "Uncategorized" : m.getCategoryId(),
                        (a, b) -> a));

        Map<String, List<Double>> scoresByCategory = new LinkedHashMap<>();
        for (UserAttempt a : attempts) {
            if (a.getTotal() <= 0) continue;
            String category = categoryByModule.getOrDefault(a.getModuleId(), "Uncategorized");
            scoresByCategory.computeIfAbsent(category, k -> new ArrayList<>())
                    .add(a.getScore() * 100.0 / a.getTotal());
        }

        return scoresByCategory.entrySet().stream()
                .map(e -> {
                    double avg = e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getKey());
                    m.put("pct", Math.round(avg * 10) / 10.0);
                    return m;
                })
                .sorted((a, b) -> Double.compare((double) b.get("pct"), (double) a.get("pct")))
                .collect(Collectors.toList());
    }

    /*
     * Average assessment score (%) per department, sorted highest-first.
     * This is what's derivable and honest — there's no "AI readiness" model,
     * so we surface actual assessment performance grouped by department.
     */
    private List<Map<String, Object>> departmentReadinessIndex(List<StudentRegistration> students, List<UserAttempt> attempts) {
        Map<String, List<Double>> scoresByEmail = attempts.stream()
                .filter(a -> a.getTotal() > 0)
                .collect(Collectors.groupingBy(
                        UserAttempt::getUserEmail,
                        Collectors.mapping(a -> a.getScore() * 100.0 / a.getTotal(), Collectors.toList())
                ));

        Map<String, List<Double>> scoresByDept = new LinkedHashMap<>();
        for (StudentRegistration s : students) {
            String dept = (s.getDepartment() == null || s.getDepartment().isBlank()) ? "Unspecified" : s.getDepartment();
            List<Double> studentScores = scoresByEmail.getOrDefault(s.getEmail(), List.of());
            if (studentScores.isEmpty()) continue;
            double avg = studentScores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            scoresByDept.computeIfAbsent(dept, k -> new ArrayList<>()).add(avg);
        }

        return scoresByDept.entrySet().stream()
                .map(e -> {
                    double avg = e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("department", e.getKey());
                    m.put("averageScorePct", Math.round(avg * 10) / 10.0);
                    m.put("studentsWithAttempts", e.getValue().size());
                    return m;
                })
                .sorted((a, b) -> Double.compare((double) b.get("averageScorePct"), (double) a.get("averageScorePct")))
                .collect(Collectors.toList());
    }

    private String bucketForCourse(List<CourseProgress> rows) {
        if (rows.isEmpty()) return "Not Started";
        double avgPct = rows.stream().mapToInt(CourseProgress::getProgressPct).average().orElse(0);
        if (avgPct >= 100) return "Completed";
        if (avgPct > 0) return "In Progress";
        return "Not Started";
    }

    // ── Courses table ────────────────────────────────────────────────────────

    public Map<String, Object> getCourses(String search, String category, String status, int page, int size) {
        List<CourseProgress> allProgress = progressRepo.findAll();
        Map<String, List<CourseProgress>> progressByCourse = allProgress.stream()
                .collect(Collectors.groupingBy(CourseProgress::getCourseId));

        List<Map<String, Object>> allRows = courseRepo.findAll().stream()
                .map(c -> {
                    List<CourseProgress> rowsForCourse = progressByCourse.getOrDefault(c.getId(), List.of());
                    double avgPct = rowsForCourse.isEmpty() ? 0 :
                            rowsForCourse.stream().mapToInt(CourseProgress::getProgressPct).average().orElse(0);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("courseId", c.getId());
                    m.put("courseName", c.getTitle());
                    // No per-course "department" concept exists — category (On-Demand/Online/Offline)
                    // is the closest real, honest grouping we have at the course level.
                    m.put("category", c.getCategory());
                    m.put("students", rowsForCourse.size());
                    m.put("status", bucketForCourse(rowsForCourse));
                    m.put("completionPct", Math.round(avgPct * 10) / 10.0);
                    return m;
                })
                .collect(Collectors.toList());

        // Summary always reflects the whole catalog, independent of the active filters below.
        long completed = allRows.stream().filter(m -> "Completed".equals(m.get("status"))).count();
        long inProgress = allRows.stream().filter(m -> "In Progress".equals(m.get("status"))).count();
        long notStarted = allRows.stream().filter(m -> "Not Started".equals(m.get("status"))).count();

        List<Map<String, Object>> rows = allRows.stream()
                .filter(m -> search == null || search.isBlank()
                        || ((String) m.get("courseName")).toLowerCase().contains(search.toLowerCase()))
                .filter(m -> category == null || category.isBlank() || category.equalsIgnoreCase((String) m.get("category")))
                .filter(m -> status == null || status.isBlank() || status.equalsIgnoreCase((String) m.get("status")))
                .sorted(Comparator.comparing(m -> (String) m.get("courseName")))
                .collect(Collectors.toList());

        Map<String, Object> result = paginate(rows, page, size);
        result.put("summary", Map.of(
                "total", allRows.size(),
                "completed", completed,
                "inProgress", inProgress,
                "notStarted", notStarted
        ));
        return result;
    }

    // ── Students table ───────────────────────────────────────────────────────

    public Map<String, Object> getStudents(String search, String department, String year, String status, int page, int size) {
        List<UserAttempt> allAttempts = attemptRepo.findAll();
        List<CourseProgress> allProgress = progressRepo.findAll();
        long totalModules = moduleRepo.count();

        Map<String, List<UserAttempt>> attemptsByEmail = allAttempts.stream()
                .collect(Collectors.groupingBy(UserAttempt::getUserEmail));
        Map<String, List<CourseProgress>> progressByEmail = allProgress.stream()
                .collect(Collectors.groupingBy(CourseProgress::getUserEmail));

        List<Map<String, Object>> rows = registrationRepo.findAll().stream()
                .map(s -> {
                    List<UserAttempt> myAttempts = attemptsByEmail.getOrDefault(s.getEmail(), List.of());
                    List<CourseProgress> myProgress = progressByEmail.getOrDefault(s.getEmail(), List.of());

                    double readiness = myAttempts.isEmpty() ? 0 : myAttempts.stream()
                            .filter(a -> a.getTotal() > 0)
                            .mapToDouble(a -> a.getScore() * 100.0 / a.getTotal())
                            .average().orElse(0);

                    long coursesCompleted = myProgress.stream()
                            .filter(p -> "COMPLETED".equals(p.getStatus()) || "CERTIFIED".equals(p.getStatus()))
                            .count();

                    // Honest, derivable status — not the fabricated Active/Graduated/On-Hold
                    // categories from the mockup, since nothing in this system tracks that yet.
                    String derivedStatus;
                    if (myProgress.isEmpty() && myAttempts.isEmpty()) {
                        derivedStatus = "Not Started";
                    } else if (!myProgress.isEmpty() && myProgress.stream().allMatch(p ->
                            "COMPLETED".equals(p.getStatus()) || "CERTIFIED".equals(p.getStatus()))) {
                        derivedStatus = "Completed";
                    } else {
                        derivedStatus = "Active";
                    }

                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("studentId", "STU" + s.getId());
                    m.put("name", s.getFullName());
                    m.put("email", s.getEmail());
                    m.put("department", s.getDepartment() == null || s.getDepartment().isBlank() ? "Unspecified" : s.getDepartment());
                    m.put("year", s.getCurrentYear() == null || s.getCurrentYear().isBlank() ? "Unspecified" : s.getCurrentYear());
                    m.put("status", derivedStatus);
                    m.put("readinessPct", Math.round(readiness * 10) / 10.0);
                    m.put("assessmentsCompleted", myAttempts.size());
                    m.put("assessmentsTotal", totalModules);
                    m.put("coursesCompleted", coursesCompleted);
                    m.put("coursesTotal", myProgress.size());
                    return m;
                })
                .filter(m -> search == null || search.isBlank()
                        || ((String) m.get("name")).toLowerCase().contains(search.toLowerCase())
                        || ((String) m.get("email")).toLowerCase().contains(search.toLowerCase())
                        || ((String) m.get("studentId")).toLowerCase().contains(search.toLowerCase()))
                .filter(m -> department == null || department.isBlank() || department.equalsIgnoreCase((String) m.get("department")))
                .filter(m -> year == null || year.isBlank() || year.equalsIgnoreCase((String) m.get("year")))
                .filter(m -> status == null || status.isBlank() || status.equalsIgnoreCase((String) m.get("status")))
                .sorted(Comparator.comparing(m -> (String) m.get("name")))
                .collect(Collectors.toList());

        return paginate(rows, page, size);
    }

    // ── Assessments table ────────────────────────────────────────────────────

    public Map<String, Object> getAssessments(String search, String type, String department, int page, int size) {
        List<UserAttempt> allAttempts = attemptRepo.findAll();
        List<StudentRegistration> students = registrationRepo.findAll();

        Map<String, String> deptByEmail = students.stream()
                .collect(Collectors.toMap(
                        StudentRegistration::getEmail,
                        s -> s.getDepartment() == null || s.getDepartment().isBlank() ? "Unspecified" : s.getDepartment(),
                        (a, b) -> a));

        Map<String, List<UserAttempt>> attemptsByModule = allAttempts.stream()
                .collect(Collectors.groupingBy(UserAttempt::getModuleId));

        List<Map<String, Object>> rows = moduleRepo.findAll().stream()
                .map(mod -> {
                    List<UserAttempt> moduleAttempts = attemptsByModule.getOrDefault(mod.getId(), List.of());

                    Set<String> distinctStudents = moduleAttempts.stream()
                            .map(UserAttempt::getUserEmail).collect(Collectors.toSet());

                    // Most common department among students who attempted this module —
                    // an honest derived grouping, since modules aren't assigned a department directly.
                    Map<String, Long> deptCounts = moduleAttempts.stream()
                            .map(a -> deptByEmail.getOrDefault(a.getUserEmail(), "Unspecified"))
                            .collect(Collectors.groupingBy(d -> d, Collectors.counting()));
                    String topDept = deptCounts.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse("Unspecified");

                    double avgPct = moduleAttempts.stream()
                            .filter(a -> a.getTotal() > 0)
                            .mapToDouble(a -> a.getScore() * 100.0 / a.getTotal())
                            .average().orElse(0);

                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("assessmentId", mod.getId());
                    m.put("name", mod.getTitle());
                    m.put("type", mod.getCategoryId() == null || mod.getCategoryId().isBlank() ? "Uncategorized" : mod.getCategoryId());
                    m.put("department", topDept);
                    m.put("students", distinctStudents.size());
                    m.put("completedPct", Math.round(avgPct * 10) / 10.0);
                    return m;
                })
                .filter(m -> search == null || search.isBlank()
                        || ((String) m.get("name")).toLowerCase().contains(search.toLowerCase()))
                .filter(m -> type == null || type.isBlank() || type.equalsIgnoreCase((String) m.get("type")))
                .filter(m -> department == null || department.isBlank() || department.equalsIgnoreCase((String) m.get("department")))
                .sorted(Comparator.comparing(m -> (String) m.get("name")))
                .collect(Collectors.toList());

        Map<String, Object> result = paginate(rows, page, size);
        result.put("summary", Map.of(
                "totalAssessments", (long) moduleRepo.count(),
                "totalAttempts", allAttempts.size(),
                "studentsAttempted", allAttempts.stream().map(UserAttempt::getUserEmail).collect(Collectors.toSet()).size(),
                "averageScorePct", Math.round(allAttempts.stream()
                        .filter(a -> a.getTotal() > 0)
                        .mapToDouble(a -> a.getScore() * 100.0 / a.getTotal())
                        .average().orElse(0) * 10) / 10.0
        ));
        return result;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> paginate(List<Map<String, Object>> rows, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 10 : size;
        int from = Math.min(safePage * safeSize, rows.size());
        int to = Math.min(from + safeSize, rows.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", rows.subList(from, to));
        result.put("total", rows.size());
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("totalPages", (int) Math.ceil(rows.size() / (double) safeSize));
        return result;
    }
}
