package com.webapp.cognitodemo.repo;

import com.webapp.cognitodemo.entity.support.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepo extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findBySubmittedByEmailOrderByCreatedAtDesc(String submittedByEmail);
}
