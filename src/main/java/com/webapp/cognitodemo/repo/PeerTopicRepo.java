package com.webapp.cognitodemo.repo;

import com.webapp.cognitodemo.entity.peer.PeerTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PeerTopicRepo extends JpaRepository<PeerTopic, Long> {
    List<PeerTopic> findByTeacherEmailOrderByCreatedAtDesc(String teacherEmail);
    List<PeerTopic> findByStatusOrderByCreatedAtDesc(String status);
}
