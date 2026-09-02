package com.webapp.cognitodemo.repo;

import com.webapp.cognitodemo.entity.peer.PeerReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PeerReviewRepo extends JpaRepository<PeerReview, Long> {
    Optional<PeerReview> findBySessionRequestId(Long sessionRequestId);
    List<PeerReview> findByTeacherEmailOrderByCreatedAtDesc(String teacherEmail);
}
