package com.example.demo.model;

import java.time.LocalDateTime;

import com.example.demo.enums.EventStatus;
import com.example.demo.enums.EventType;
import com.example.demo.enums.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Event {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated
	private TransactionType transactionType;
	@Column
	private String transactionId;
	@Enumerated
	private EventType eventType;
	@Column
	private String payload;

	@Enumerated
	private EventStatus status;
	@Column
	private LocalDateTime createdAt;
	@Column
	private LocalDateTime publishedAt;
	
	
	@Column
	private LocalDateTime lastAttemptAt;
	
	@Column
	private int retryCount;
	
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	public String getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	
	public String getPayload() {
		return payload;
	}
	public void setPayload(String payload) {
		this.payload = payload;
	}
	public EventStatus getStatus() {
		return status;
	}
	public void setStatus(EventStatus status) {
		this.status = status;
	}
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	public LocalDateTime getPublishedAt() {
		return publishedAt;
	}
	public void setPublishedAt(LocalDateTime publishedAt) {
		this.publishedAt = publishedAt;
	}
	
	
	public EventType getEventType() {
		return eventType;
	}
	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}
	
	
	
	public TransactionType getTransactionType() {
		return transactionType;
	}
	public void setTransactionType(TransactionType transactionType) {
		this.transactionType = transactionType;
	}

	
	public LocalDateTime getLastAttemptAt() {
		return lastAttemptAt;
	}
	public void setLastAttemptAt(LocalDateTime lastAttemptAt) {
		this.lastAttemptAt = lastAttemptAt;
	}
	public int getRetryCount() {
		return retryCount;
	}
	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}
	public Event(TransactionType transactionType, String transactionId, EventType eventType, String payload,
			EventStatus status, LocalDateTime createdAt, LocalDateTime publishedAt, LocalDateTime lastAttemptAt,
			int retryCount) {
		super();
		this.transactionType = transactionType;
		this.transactionId = transactionId;
		this.eventType = eventType;
		this.payload = payload;
		this.status = status;
		this.createdAt = createdAt;
		this.publishedAt = publishedAt;
		this.lastAttemptAt = lastAttemptAt;
		this.retryCount = retryCount;
	}
	public Event(Long id, TransactionType transactionType, String transactionId, EventType eventType, String payload,
			EventStatus status, LocalDateTime createdAt, LocalDateTime publishedAt, LocalDateTime lastAttemptAt,
			int retryCount) {
		super();
		this.id = id;
		this.transactionType = transactionType;
		this.transactionId = transactionId;
		this.eventType = eventType;
		this.payload = payload;
		this.status = status;
		this.createdAt = createdAt;
		this.publishedAt = publishedAt;
		this.lastAttemptAt = lastAttemptAt;
		this.retryCount = retryCount;
	}
	public Event() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	
	
}
