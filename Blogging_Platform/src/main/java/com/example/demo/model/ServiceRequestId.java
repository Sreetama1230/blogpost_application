package com.example.demo.model;


import com.example.demo.enums.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class ServiceRequestId {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column
	private long elementid;
	
	@Column
	private String requestid;
	
	@Enumerated(EnumType.STRING)
	private TransactionType transactionType;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}


	public String getRequestid() {
		return requestid;
	}

	public void setRequestid(String requestid) {
		this.requestid = requestid;
	}

	public TransactionType getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(TransactionType transactionType) {
		this.transactionType = transactionType;
	}


	public ServiceRequestId(long elementid, String requestid, TransactionType transactionType) {
		super();
		this.elementid = elementid;
		this.requestid = requestid;
		this.transactionType = transactionType;
	}

	public ServiceRequestId() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	
}
