package com.webapp.cognitodemo.service;

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

            courseRepo.findById(cp.getCourseId()).ifPresent(c -> {
                m.put("title",       c.getTitle());
                m.put("instructor",  c.getInstructor());
                m.put("duration",    c.getDuration());
                m.put("category",    c.getCategory());
                m.put("imageUrl",    c.getImageUrl());
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

    public void startCourse(String email, String courseId) {
        progressRepo.findByUserEmailAndCourseId(email, courseId).ifPresent(cp -> {
            if ("REGISTERED".equals(cp.getStatus())) {
                cp.setStatus("IN_PROGRESS");
                cp.setProgressPct(5);
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
}
