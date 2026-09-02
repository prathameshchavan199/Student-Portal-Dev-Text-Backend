package com.webapp.cognitodemo.repo;

import com.webapp.cognitodemo.entity.peer.PeerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PeerProfileRepo extends JpaRepository<PeerProfile, Long> {
    Optional<PeerProfile> findByEmail(String email);
    boolean existsByEmail(String email);
}
