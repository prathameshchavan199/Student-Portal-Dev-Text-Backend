package com.webapp.cognitodemo.service;

import com.webapp.cognitodemo.entity.User;
import com.webapp.cognitodemo.entity.assessment.AssessmentCategory;
import com.webapp.cognitodemo.entity.assessment.AssessmentModule;
import com.webapp.cognitodemo.entity.course.Course;
import com.webapp.cognitodemo.entity.course.CourseProgress;
import com.webapp.cognitodemo.entity.assessment.UserAttempt;
import com.webapp.cognitodemo.entity.registration.StudentRegistration;
import com.webapp.cognitodemo.repo.AssessmentCategoryRepo;
import com.webapp.cognitodemo.repo.AssessmentModuleRepo;
import com.webapp.cognitodemo.repo.CourseProgressRepo;
import com.webapp.cognitodemo.repo.CourseRepo;
import com.webapp.cognitodemo.repo.RegistrationRepo;
import com.webapp.cognitodemo.repo.UserAttemptRepo;
import com.webapp.cognitodemo.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/*
 * Backs the TPO (Training & Placement Officer) admin panel.
 *
 * Every number here is derived from data that genuinely exists in this
 * system (courses, course-progress, student registrations, assessment
 * attempts) — there is no placement/campus-drive/offers data model yet,
 * so that section is intentionally left out rather than faked.
 *
 * College scoping: a TPO admin only ever sees students whose undergraduate
 * OR postgraduate college matches the admin's own `college` (set on the
 * User entity). Every method below works from a college-scoped student
 * list so course/assessment stats never leak another college's data.
 */
@Service
public class TpoService {

    @Autowired private CourseRepo courseRepo;
    @Autowired private CourseProgressRepo progressRepo;
    @Autowired private RegistrationRepo registrationRepo;
    @Autowired private UserAttemptRepo attemptRepo;
    @Autowired private AssessmentModuleRepo moduleRepo;
    @Autowired private AssessmentCategoryRepo categoryRepo;
    @Autowired private UserRepo userRepo;

    /* The logged-in TPO admin's college, or null if they have none set / aren't authenticated. */
    private String currentAdminCollege() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return userRepo.findByEmail(auth.getName())
                .map(User::getCollege)
                .filter(c -> c != null && !c.isBlank())
                .orElse(null);
    }

    /*
     * All students whose undergraduate or postgraduate college matches the
     * current TPO admin's college. If the admin has no college configured,
     * returns an empty list — no college means no visible students, rather
     * than accidentally showing everyone.
     */
    private List<StudentRegistration> scopedStudents() {
        String college = currentAdminCollege();
        if (college == null) return List.of();
        return registrationRepo.findAll().stream()
                .filter(s -> college.equalsIgnoreCase(s.getUndergraduateUniversity())
                        || college.equalsIgnoreCase(s.getPostGraduationUniversity()))
                .collect(Collectors.toList());
    }

    private Set<String> emailsOf(List<StudentRegistration> students) {
        return students.stream().map(StudentRegistration::getEmail).collect(Collectors.toSet());
    }

    // ── Dashboard ────────────────────────────────────────────────────────────

    public Map<String, Object> getDashboard() {
        List<Course> courses = courseRepo.findAll();
        List<StudentRegistration> students = scopedStudents();
        Set<String> emails = emailsOf(students);
        List<CourseProgress> allProgress = progressRepo.findAll().stream()
                .filter(p -> emails.contains(p.getUserEmail()))
                .collect(Collectors.toList());
        List<UserAttempt> attempts = attemptRepo.findAll().stream()
                .filter(a -> emails.contains(a.getUserEmail()))
                .collect(Collectors.toList());
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
        stats.put("degreeReadiness", degreeReadinessIndex(students, attempts));
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
     * Groups a student by their undergraduate degree (e.g. "B.Tech", "BCA"),
     * falling back to their custom entry when the degree is "Other".
     * Replaces the old department-based grouping across the TPO admin panel.
     */
    private String degreeLabel(StudentRegistration s) {
        if (s.getHasUndergraduate() == null || !s.getHasUndergraduate()) return "Unspecified";
        String degree = s.getUndergraduateDegree();
        if (degree == null || degree.isBlank()) return "Unspecified";
        if ("Other".equalsIgnoreCase(degree)) {
            String other = s.getUndergraduateOtherDegree();
            return (other == null || other.isBlank()) ? "Other" : other;
        }
        return degree;
    }

    /*
     * Average assessment score (%) per undergraduate degree, sorted highest-first.
     * This is what's derivable and honest — there's no "AI readiness" model,
     * so we surface actual assessment performance grouped by degree.
     */
    private List<Map<String, Object>> degreeReadinessIndex(List<StudentRegistration> students, List<UserAttempt> attempts) {
        Map<String, List<Double>> scoresByEmail = attempts.stream()
                .filter(a -> a.getTotal() > 0)
                .collect(Collectors.groupingBy(
                        UserAttempt::getUserEmail,
                        Collectors.mapping(a -> a.getScore() * 100.0 / a.getTotal(), Collectors.toList())
                ));

        Map<String, List<Double>> scoresByDegree = new LinkedHashMap<>();
        for (StudentRegistration s : students) {
            String degree = degreeLabel(s);
            List<Double> studentScores = scoresByEmail.getOrDefault(s.getEmail(), List.of());
            if (studentScores.isEmpty()) continue;
            double avg = studentScores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            scoresByDegree.computeIfAbsent(degree, k -> new ArrayList<>()).add(avg);
        }

        return scoresByDegree.entrySet().stream()
                .map(e -> {
                    double avg = e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("degree", e.getKey());
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

    public Map<String, Object> getCourses(String search, String category, String degree, String status, int page, int size) {
        List<StudentRegistration> students = scopedStudents();
        Set<String> emails = emailsOf(students);
        List<CourseProgress> allProgress = progressRepo.findAll().stream()
                .filter(p -> emails.contains(p.getUserEmail()))
                .collect(Collectors.toList());
        Map<String, List<CourseProgress>> progressByCourse = allProgress.stream()
                .collect(Collectors.groupingBy(CourseProgress::getCourseId));

        Map<String, String> degreeByEmail = students.stream()
                .collect(Collectors.toMap(
                        StudentRegistration::getEmail,
                        this::degreeLabel,
                        (a, b) -> a));

        List<Map<String, Object>> allRows = courseRepo.findAll().stream()
                .map(c -> {
                    List<CourseProgress> rowsForCourse = progressByCourse.getOrDefault(c.getId(), List.of());
                    double avgPct = rowsForCourse.isEmpty() ? 0 :
                            rowsForCourse.stream().mapToInt(CourseProgress::getProgressPct).average().orElse(0);
                    long completedStudents = rowsForCourse.stream()
                            .filter(cp -> cp.getProgressPct() >= 100)
                            .count();

                    // Most common undergraduate degree among students enrolled in this course —
                    // an honest derived grouping, since courses aren't assigned a degree directly.
                    Map<String, Long> degreeCounts = rowsForCourse.stream()
                            .map(cp -> degreeByEmail.getOrDefault(cp.getUserEmail(), "Unspecified"))
                            .collect(Collectors.groupingBy(d -> d, Collectors.counting()));
                    String topDegree = degreeCounts.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse("Unspecified");

                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("courseId", c.getId());
                    m.put("courseName", c.getTitle());
                    m.put("category", c.getCategory());
                    m.put("degree", topDegree);
                    m.put("students", rowsForCourse.size());
                    m.put("status", bucketForCourse(rowsForCourse));
                    m.put("completionPct", Math.round(avgPct * 10) / 10.0);
                    m.put("completedStudents", completedStudents);
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
                .filter(m -> degree == null || degree.isBlank() || degree.equalsIgnoreCase((String) m.get("degree")))
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

    public Map<String, Object> getStudents(String search, String degree, String year, String status, int page, int size) {
        List<StudentRegistration> students = scopedStudents();
        Set<String> emails = emailsOf(students);
        List<UserAttempt> allAttempts = attemptRepo.findAll().stream()
                .filter(a -> emails.contains(a.getUserEmail()))
                .collect(Collectors.toList());
        List<CourseProgress> allProgress = progressRepo.findAll().stream()
                .filter(p -> emails.contains(p.getUserEmail()))
                .collect(Collectors.toList());
        long totalModules = moduleRepo.count();

        Map<String, List<UserAttempt>> attemptsByEmail = allAttempts.stream()
                .collect(Collectors.groupingBy(UserAttempt::getUserEmail));
        Map<String, List<CourseProgress>> progressByEmail = allProgress.stream()
                .collect(Collectors.groupingBy(CourseProgress::getUserEmail));

        List<Map<String, Object>> rows = students.stream()
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
                    m.put("degree", degreeLabel(s));
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
                .filter(m -> degree == null || degree.isBlank() || degree.equalsIgnoreCase((String) m.get("degree")))
                .filter(m -> year == null || year.isBlank() || year.equalsIgnoreCase((String) m.get("year")))
                .filter(m -> status == null || status.isBlank() || status.equalsIgnoreCase((String) m.get("status")))
                .sorted(Comparator.comparing(m -> (String) m.get("name")))
                .collect(Collectors.toList());

        return paginate(rows, page, size);
    }

    // ── Assessments table ────────────────────────────────────────────────────

    public Map<String, Object> getAssessments(String search, String degree, int page, int size) {
        List<StudentRegistration> students = scopedStudents();
        Set<String> emails = emailsOf(students);
        List<UserAttempt> allAttempts = attemptRepo.findAll().stream()
                .filter(a -> emails.contains(a.getUserEmail()))
                .collect(Collectors.toList());

        Map<String, String> degreeByEmail = students.stream()
                .collect(Collectors.toMap(
                        StudentRegistration::getEmail,
                        this::degreeLabel,
                        (a, b) -> a));

        // Every module belongs to one of the 4 assessment categories
        // (Technical Skills / Problem Solving / Communication / Data Skills).
        // Within each category, the admin panel shows one row PER undergraduate
        // degree that has attempted it — e.g. "Problem Solving" for B.Tech and
        // "Problem Solving" for B.Sc are two separate rows.
        Map<String, String> categoryByModule = moduleRepo.findAll().stream()
                .collect(Collectors.toMap(
                        AssessmentModule::getId,
                        m -> m.getCategoryId() == null || m.getCategoryId().isBlank() ? "uncategorized" : m.getCategoryId(),
                        (a, b) -> a));

        // categoryId -> degree -> attempts
        Map<String, Map<String, List<UserAttempt>>> attemptsByCategoryAndDegree = new LinkedHashMap<>();
        for (UserAttempt a : allAttempts) {
            String catId = categoryByModule.getOrDefault(a.getModuleId(), "uncategorized");
            String deg = degreeByEmail.getOrDefault(a.getUserEmail(), "Unspecified");
            attemptsByCategoryAndDegree
                    .computeIfAbsent(catId, k -> new LinkedHashMap<>())
                    .computeIfAbsent(deg, k -> new ArrayList<>())
                    .add(a);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        // Parallel list tracking when each row's (category, degree) combo first
        // appeared (earliest attempt timestamp) — used to put the most recently
        // introduced row at the top. Rows with no attempts yet sort to the bottom.
        List<LocalDateTime> rowFirstSeenAt = new ArrayList<>();

        for (AssessmentCategory cat : categoryRepo.findAll().stream()
                .sorted(Comparator.comparingInt(AssessmentCategory::getDisplayOrder))
                .collect(Collectors.toList())) {

            Map<String, List<UserAttempt>> byDegree = attemptsByCategoryAndDegree.getOrDefault(cat.getId(), Map.of());

            if (byDegree.isEmpty()) {
                // No attempts at all yet for this category — still show it so the
                // category isn't invisible, just with nothing to report.
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("assessmentId", cat.getId() + "-none");
                m.put("name", cat.getTitle());
                m.put("degree", "Unspecified");
                m.put("students", 0);
                m.put("completedPct", 0.0);
                rows.add(m);
                rowFirstSeenAt.add(null);
                continue;
            }

            for (Map.Entry<String, List<UserAttempt>> entry : byDegree.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toList())) {
                List<UserAttempt> degAttempts = entry.getValue();
                Set<String> distinctStudents = degAttempts.stream()
                        .map(UserAttempt::getUserEmail).collect(Collectors.toSet());
                double avgPct = degAttempts.stream()
                        .filter(a -> a.getTotal() > 0)
                        .mapToDouble(a -> a.getScore() * 100.0 / a.getTotal())
                        .average().orElse(0);
                LocalDateTime firstSeenAt = degAttempts.stream()
                        .map(UserAttempt::getAttemptedAt)
                        .filter(Objects::nonNull)
                        .min(Comparator.naturalOrder())
                        .orElse(null);

                Map<String, Object> m = new LinkedHashMap<>();
                m.put("assessmentId", cat.getId() + "-" + entry.getKey());
                m.put("name", cat.getTitle());
                m.put("degree", entry.getKey());
                m.put("students", distinctStudents.size());
                m.put("completedPct", Math.round(avgPct * 10) / 10.0);
                rows.add(m);
                rowFirstSeenAt.add(firstSeenAt);
            }
        }

        // Most recently introduced (category, degree) row first; rows with no
        // attempts yet (null timestamp) sink to the bottom.
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) order.add(i);
        order.sort((i1, i2) -> {
            LocalDateTime t1 = rowFirstSeenAt.get(i1);
            LocalDateTime t2 = rowFirstSeenAt.get(i2);
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.compareTo(t1);
        });
        List<Map<String, Object>> orderedRows = new ArrayList<>();
        for (int i : order) orderedRows.add(rows.get(i));
        rows = orderedRows;

        rows = rows.stream()
                .filter(m -> search == null || search.isBlank()
                        || ((String) m.get("name")).toLowerCase().contains(search.toLowerCase()))
                .filter(m -> degree == null || degree.isBlank() || degree.equalsIgnoreCase((String) m.get("degree")))
                .collect(Collectors.toList());

        Map<String, Object> result = paginate(rows, page, size);
        result.put("summary", Map.of(
                "totalAssessments", (long) categoryRepo.count(),
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
