package com.webapp.cognitodemo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webapp.cognitodemo.entity.course.Course;
import com.webapp.cognitodemo.entity.course.CourseProgress;
import com.webapp.cognitodemo.entity.payment.Payment;
import com.webapp.cognitodemo.repo.CourseProgressRepo;
import com.webapp.cognitodemo.repo.CourseRepo;
import com.webapp.cognitodemo.repo.PaymentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseProgressService {

    @Autowired private CourseProgressRepo progressRepo;
    @Autowired private PaymentRepo         paymentRepo;
    @Autowired private CourseRepo          courseRepo;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /*
     * Ensures every PAID course has a matching CourseProgress row.
     * Safe to call repeatedly — skips courses that already have a row.
     */
    public void ensureProgressRows(String email) {
        List<Payment> paid = paymentRepo.findByUserEmailAndStatus(email, "PAID");
        for (Payment p : paid) {
            boolean exists = progressRepo
                    .findByUserEmailAndCourseId(email, p.getCourseId())
                    .isPresent();
            if (!exists) {
                CourseProgress cp = new CourseProgress();
                cp.setUserEmail(email);
                cp.setCourseId(p.getCourseId());
                cp.setCourseName(p.getCourseName());
                cp.setStatus("REGISTERED");
                cp.setProgressPct(0);
                progressRepo.save(cp);
            }
        }
    }

    /*
     * Returns the full course progress list for a user, enriched with
     * course metadata from the courses table.
     */
    public List<Map<String, Object>> getUserProgress(String email) {
        ensureProgressRows(email);

        return progressRepo.findByUserEmail(email).stream().map(cp -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("courseId",    cp.getCourseId());
            m.put("courseName",  cp.getCourseName());
            m.put("status",      cp.getStatus());
            m.put("progressPct", cp.getProgressPct());
            m.put("lastLessonKey", cp.getLastLessonKey());

            courseRepo.findById(cp.getCourseId()).ifPresent(c -> {
                m.put("title",       c.getTitle());
                m.put("instructor",  c.getInstructor());
                m.put("duration",    c.getDuration());
                m.put("category",    c.getCategory());
                m.put("imageUrl",    c.getImageUrl());
                m.put("imageKey",    c.getImageKey());
                m.put("description", c.getDescription());
                m.put("date",        c.getDate());
                m.put("time",        c.getTime());
                m.put("platform",    c.getPlatform());
                m.put("startsIn",    c.getStartsIn());
                m.put("format",      c.getFormat());
            });
            return m;
        }).collect(Collectors.toList());
    }

    /*
     * Detail for a single course — used by the video-player / "Learn" page.
     * Includes which lesson keys ("moduleIndex-lessonIndex") are complete.
     */
    public Map<String, Object> getCourseProgressDetail(String email, String courseId) {
        ensureProgressRows(email);
        CourseProgress cp = progressRepo.findByUserEmailAndCourseId(email, courseId)
                .orElseThrow(() -> new NoSuchElementException("Not registered for course: " + courseId));

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("courseId",         cp.getCourseId());
        m.put("status",           cp.getStatus());
        m.put("progressPct",      cp.getProgressPct());
        m.put("completedLessons", parseLessons(cp.getCompletedLessonsJson()));
        m.put("lastLessonKey",    cp.getLastLessonKey());
        return m;
    }

    public void startCourse(String email, String courseId) {
        ensureProgressRows(email);
        progressRepo.findByUserEmailAndCourseId(email, courseId).ifPresent(cp -> {
            if ("REGISTERED".equals(cp.getStatus())) {
                cp.setStatus("IN_PROGRESS");
                cp.setProgressPct(Math.max(cp.getProgressPct(), 5));
                cp.setStartedAt(LocalDateTime.now());
                progressRepo.save(cp);
            }
        });
    }

    public void completeCourse(String email, String courseId) {
        progressRepo.findByUserEmailAndCourseId(email, courseId).ifPresent(cp -> {
            if ("IN_PROGRESS".equals(cp.getStatus())) {
                cp.setStatus("COMPLETED");
                cp.setProgressPct(100);
                cp.setCompletedAt(LocalDateTime.now());
                progressRepo.save(cp);
            }
        });
    }

    public void certifyCourse(String email, String courseId) {
        progressRepo.findByUserEmailAndCourseId(email, courseId).ifPresent(cp -> {
            if ("COMPLETED".equals(cp.getStatus())) {
                cp.setStatus("CERTIFIED");
                progressRepo.save(cp);
            }
        });
    }

    /*
     * Marks a single lesson ("moduleIndex-lessonIndex") as watched/complete,
     * recalculates the overall progressPct against the course's total lesson
     * count, and auto-promotes REGISTERED -> IN_PROGRESS -> COMPLETED.
     */
    public Map<String, Object> markLessonComplete(String email, String courseId, String lessonKey) {
        ensureProgressRows(email);
        CourseProgress cp = progressRepo.findByUserEmailAndCourseId(email, courseId)
                .orElseThrow(() -> new NoSuchElementException("Not registered for course: " + courseId));

        Set<String> completed = new LinkedHashSet<>(parseLessons(cp.getCompletedLessonsJson()));
        completed.add(lessonKey);
        cp.setCompletedLessonsJson(serializeLessons(completed));
        cp.setLastLessonKey(lessonKey);

        int totalLessons = countLessons(courseId);
        int pct = totalLessons > 0
                ? Math.min(100, (int) Math.round((completed.size() * 100.0) / totalLessons))
                : Math.max(cp.getProgressPct(), 5);

        if ("REGISTERED".equals(cp.getStatus())) {
            cp.setStatus("IN_PROGRESS");
            cp.setStartedAt(LocalDateTime.now());
        }

        cp.setProgressPct(Math.max(cp.getProgressPct(), pct));

        if (pct >= 100 && !"CERTIFIED".equals(cp.getStatus())) {
            cp.setStatus("COMPLETED");
            cp.setCompletedAt(LocalDateTime.now());
        }

        progressRepo.save(cp);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status",           cp.getStatus());
        m.put("progressPct",      cp.getProgressPct());
        m.put("completedLessons", completed);
        return m;
    }

    /*
     * Remember which lesson the learner has open, without necessarily
     * marking it complete (used when they simply open/start a lesson).
     */
    public void setLastLesson(String email, String courseId, String lessonKey) {
        ensureProgressRows(email);
        progressRepo.findByUserEmailAndCourseId(email, courseId).ifPresent(cp -> {
            cp.setLastLessonKey(lessonKey);
            if ("REGISTERED".equals(cp.getStatus())) {
                cp.setStatus("IN_PROGRESS");
                cp.setProgressPct(Math.max(cp.getProgressPct(), 5));
                cp.setStartedAt(LocalDateTime.now());
            }
            progressRepo.save(cp);
        });
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private int countLessons(String courseId) {
        return courseRepo.findById(courseId)
                .map(Course::getCurriculumJson)
                .map(this::countLessonsInCurriculumJson)
                .orElse(0);
    }

    @SuppressWarnings("unchecked")
    private int countLessonsInCurriculumJson(String json) {
        if (json == null || json.isBlank()) return 0;
        try {
            List<Map<String, Object>> modules = objectMapper.readValue(json, new TypeReference<>() {});
            int total = 0;
            for (Map<String, Object> module : modules) {
                Object lessons = module.get("lessons");
                if (lessons instanceof List<?> list) {
                    total += list.size();
                }
            }
            return total;
        } catch (Exception e) {
            return 0;
        }
    }

    private List<String> parseLessons(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String serializeLessons(Set<String> lessons) {
        try {
            return objectMapper.writeValueAsString(lessons);
        } catch (Exception e) {
            return "[]";
        }
    }
}
