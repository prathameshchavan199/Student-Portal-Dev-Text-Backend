package com.webapp.cognitodemo.repo;

import com.webapp.cognitodemo.entity.peer.PeerSessionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PeerSessionRequestRepo extends JpaRepository<PeerSessionRequest, Long> {
    List<PeerSessionRequest> findByTeacherEmailOrderByCreatedAtDesc(String teacherEmail);
    List<PeerSessionRequest> findByLearnerEmailOrderByCreatedAtDesc(String learnerEmail);
    List<PeerSessionRequest> findByTeacherEmailAndStatusOrderByCreatedAtDesc(String teacherEmail, String status);
}
