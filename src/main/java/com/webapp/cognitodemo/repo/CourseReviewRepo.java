package com.webapp.cognitodemo.repo;

import com.webapp.cognitodemo.entity.course.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseReviewRepo extends JpaRepository<CourseReview, Long> {
    List<CourseReview> findByCourseIdOrderByCreatedAtDesc(String courseId);
}
