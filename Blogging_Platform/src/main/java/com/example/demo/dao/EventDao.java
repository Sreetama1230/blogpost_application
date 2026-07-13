package com.example.demo.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.enums.EventStatus;
import com.example.demo.enums.EventType;
import com.example.demo.model.Event;

@Repository
public interface EventDao extends JpaRepository<Event, Long> {

	@Query("SELECT e FROM Event e WHERE e.retryCount < 10 AND e.status = :status ORDER BY e.createdAt")
	List<Event> findByStatus(@Param(value = "status") EventStatus status, Pageable page);

	Optional<Event> findByTransactionIdAndEventType(String transactionId, EventType eventType);
}
