package com.webapp.cognitodemo.repo;

import com.webapp.cognitodemo.entity.course.CourseProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseProgressRepo extends JpaRepository<CourseProgress, Long> {

    List<CourseProgress> findByUserEmail(String userEmail);

    Optional<CourseProgress> findByUserEmailAndCourseId(String userEmail, String courseId);
}
