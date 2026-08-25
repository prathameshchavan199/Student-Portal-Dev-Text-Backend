package com.webapp.cognitodemo.service;

import com.webapp.cognitodemo.entity.User;
import com.webapp.cognitodemo.entity.assessment.AssessmentModule;
import com.webapp.cognitodemo.entity.assessment.UserAttempt;
import com.webapp.cognitodemo.entity.course.Course;
import com.webapp.cognitodemo.entity.course.CourseProgress;
import com.webapp.cognitodemo.repo.AssessmentModuleRepo;
import com.webapp.cognitodemo.repo.CourseProgressRepo;
import com.webapp.cognitodemo.repo.CourseRepo;
import com.webapp.cognitodemo.repo.UserAttemptRepo;
import com.webapp.cognitodemo.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * Aggregates real numbers from the existing repositories (users, courses,
 * course_progress, user_attempts) into the shape the admin dashboard UI
 * needs. Nothing here is mocked - it's all computed on read, which is fine
 * at this data scale; if the tables grow large this should move to
 * scheduled/cached rollups instead of computing on every request.
 */
@Service
public class AdminDashboardService {

    @Autowired private UserRepo userRepo;
    @Autowired private CourseRepo courseRepo;
    @Autowired private CourseProgressRepo courseProgressRepo;
    @Autowired private UserAttemptRepo userAttemptRepo;
    @Autowired private AssessmentModuleRepo assessmentModuleRepo;

    public Map<String, Object> getDashboard() {
        List<User> users = userRepo.findAll();
        List<Course> courses = courseRepo.findAll();
        List<CourseProgress> progressRecords = courseProgressRepo.findAll();
        List<UserAttempt> attempts = userAttemptRepo.findAll();
        List<AssessmentModule> modules = assessmentModuleRepo.findAll();

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("summary", buildSummary(users, courses, progressRecords, attempts));
        dashboard.put("enrollmentStatus", buildEnrollmentStatus(progressRecords));
        dashboard.put("enrollmentTrend", buildEnrollmentTrend(progressRecords));
        dashboard.put("topCourses", buildTopCourses(courses, progressRecords));
        dashboard.put("assessmentPerformance", buildAssessmentPerformance(modules, attempts));
        dashboard.put("activity", buildActivity(progressRecords, attempts));
        return dashboard;
    }

    private Map<String, Object> buildSummary(List<User> users, List<Course> courses,
                                              List<CourseProgress> progressRecords, List<UserAttempt> attempts) {
        int totalStudents = users.size();
        int totalCourses = courses.size();
        int totalEnrollments = progressRecords.size();

        long completedOrCertified = progressRecords.stream()
                .filter(p -> "COMPLETED".equalsIgnoreCase(p.getStatus()) || "CERTIFIED".equalsIgnoreCase(p.getStatus()))
                .count();
        double completionRate = totalEnrollments == 0 ? 0 : (completedOrCertified * 100.0) / totalEnrollments;

        long totalScore = attempts.stream().mapToLong(UserAttempt::getScore).sum();
        long totalPossible = attempts.stream().mapToLong(UserAttempt::getTotal).sum();
        double avgAssessmentScore = totalPossible == 0 ? 0 : (totalScore * 100.0) / totalPossible;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalStudents", totalStudents);
        summary.put("totalCourses", totalCourses);
        summary.put("totalEnrollments", totalEnrollments);
        summary.put("courseCompletionRatePct", round1(completionRate));
        summary.put("avgAssessmentScorePct", round1(avgAssessmentScore));
        summary.put("totalAssessmentAttempts", attempts.size());
        return summary;
    }

    private Map<String, Object> buildEnrollmentStatus(List<CourseProgress> progressRecords) {
        Map<String, Long> counts = progressRecords.stream()
                .collect(Collectors.groupingBy(
                        p -> normalizeStatus(p.getStatus()),
                        LinkedHashMap::new,
                        Collectors.counting()));

        List<String> order = List.of("Registered", "In Progress", "Completed", "Certified");
        List<Map<String, Object>> segments = new ArrayList<>();
        int total = progressRecords.size();
        for (String label : order) {
            long count = counts.getOrDefault(label, 0L);
            Map<String, Object> segment = new LinkedHashMap<>();
            segment.put("label", label);
            segment.put("count", count);
            segment.put("pct", total == 0 ? 0 : round1((count * 100.0) / total));
            segments.add(segment);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("segments", segments);
        return result;
    }

    private List<Map<String, Object>> buildEnrollmentTrend(List<CourseProgress> progressRecords) {
        // Last 6 months (including current), counting new enrollments (createdAt) per month.
        LocalDateTime now = LocalDateTime.now();
        Map<String, Long> byMonth = new LinkedHashMap<>();
        List<String> monthKeys = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime month = now.minusMonths(i);
            String key = month.getYear() + "-" + month.getMonthValue();
            monthKeys.add(key);
            byMonth.put(key, 0L);
        }

        for (CourseProgress p : progressRecords) {
            if (p.getCreatedAt() == null) continue;
            String key = p.getCreatedAt().getYear() + "-" + p.getCreatedAt().getMonthValue();
            if (byMonth.containsKey(key)) {
                byMonth.put(key, byMonth.get(key) + 1);
            }
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        long cumulative = 0;
        for (String key : monthKeys) {
            long monthCount = byMonth.get(key);
            cumulative += monthCount;
            int monthValue = Integer.parseInt(key.split("-")[1]);
            String label = java.time.Month.of(monthValue).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("label", label);
            point.put("newEnrollments", monthCount);
            point.put("cumulativeEnrollments", cumulative);
            trend.add(point);
        }
        return trend;
    }

    private List<Map<String, Object>> buildTopCourses(List<Course> courses, List<CourseProgress> progressRecords) {
        Map<String, List<CourseProgress>> byCourse = progressRecords.stream()
                .collect(Collectors.groupingBy(CourseProgress::getCourseId));

        Map<String, String> titleById = courses.stream()
                .collect(Collectors.toMap(Course::getId, Course::getTitle, (a, b) -> a));

        return byCourse.entrySet().stream()
                .map(entry -> {
                    String courseId = entry.getKey();
                    List<CourseProgress> records = entry.getValue();
                    long completed = records.stream()
                            .filter(p -> "COMPLETED".equalsIgnoreCase(p.getStatus()) || "CERTIFIED".equalsIgnoreCase(p.getStatus()))
                            .count();
                    double completionPct = records.isEmpty() ? 0 : (completed * 100.0) / records.size();

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("courseId", courseId);
                    row.put("title", titleById.getOrDefault(courseId, courseId));
                    row.put("enrolled", records.size());
                    row.put("completionPct", round1(completionPct));
                    return row;
                })
                .sorted(Comparator.comparingInt((Map<String, Object> r) -> (int) r.get("enrolled")).reversed())
                .limit(6)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildAssessmentPerformance(List<AssessmentModule> modules, List<UserAttempt> attempts) {
        Map<String, String> titleById = modules.stream()
                .collect(Collectors.toMap(AssessmentModule::getId, AssessmentModule::getTitle, (a, b) -> a));

        Map<String, List<UserAttempt>> byModule = attempts.stream()
                .collect(Collectors.groupingBy(UserAttempt::getModuleId));

        return byModule.entrySet().stream()
                .map(entry -> {
                    String moduleId = entry.getKey();
                    List<UserAttempt> moduleAttempts = entry.getValue();
                    long score = moduleAttempts.stream().mapToLong(UserAttempt::getScore).sum();
                    long total = moduleAttempts.stream().mapToLong(UserAttempt::getTotal).sum();
                    double avgPct = total == 0 ? 0 : (score * 100.0) / total;

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("moduleId", moduleId);
                    row.put("title", titleById.getOrDefault(moduleId, moduleId));
                    row.put("attempts", moduleAttempts.size());
                    row.put("avgScorePct", round1(avgPct));
                    return row;
                })
                .sorted(Comparator.comparingDouble((Map<String, Object> r) -> (double) r.get("avgScorePct")).reversed())
                .limit(6)
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildActivity(List<CourseProgress> progressRecords, List<UserAttempt> attempts) {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        long activeLearners = progressRecords.stream()
                .map(CourseProgress::getUserEmail)
                .distinct()
                .count();

        long certificatesIssued = progressRecords.stream()
                .filter(p -> "CERTIFIED".equalsIgnoreCase(p.getStatus()))
                .count();

        long attemptsLast7Days = attempts.stream()
                .filter(a -> a.getAttemptedAt() != null && a.getAttemptedAt().isAfter(weekAgo))
                .count();

        Map<String, Object> activity = new LinkedHashMap<>();
        activity.put("activeLearners", activeLearners);
        activity.put("certificatesIssued", certificatesIssued);
        activity.put("assessmentAttemptsLast7Days", attemptsLast7Days);
        return activity;
    }

    public List<Map<String, Object>> getStudents() {
        List<User> users = userRepo.findAll();
        List<CourseProgress> progressRecords = courseProgressRepo.findAll();

        Map<String, List<CourseProgress>> byEmail = progressRecords.stream()
                .collect(Collectors.groupingBy(CourseProgress::getUserEmail));

        return users.stream()
                .map(user -> {
                    List<CourseProgress> records = byEmail.getOrDefault(user.getEmail(), List.of());
                    long completed = records.stream()
                            .filter(p -> "COMPLETED".equalsIgnoreCase(p.getStatus()) || "CERTIFIED".equalsIgnoreCase(p.getStatus()))
                            .count();

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("fullName", user.getFullName());
                    row.put("email", user.getEmail());
                    row.put("registrationComplete", user.isRegistration());
                    row.put("provider", user.getProvider());
                    row.put("enrolledCourses", records.size());
                    row.put("completedCourses", completed);
                    return row;
                })
                .sorted(Comparator.comparing((Map<String, Object> r) -> (String) r.get("fullName"), Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    private String normalizeStatus(String status) {
        if (status == null) return "Registered";
        return switch (status.toUpperCase()) {
            case "IN_PROGRESS" -> "In Progress";
            case "COMPLETED" -> "Completed";
            case "CERTIFIED" -> "Certified";
            default -> "Registered";
        };
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
