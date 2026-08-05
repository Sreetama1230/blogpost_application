package com.example.demo.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.enums.TransactionType;
import com.example.demo.model.ServiceRequestId;

public interface ServiceRequestIdDao extends JpaRepository<ServiceRequestId, Long> {

	@Query("SELECT s.elementid from ServiceRequestId s where  s.requestid=:reqId and  s.transactionType = :transactionType")
	public Optional<Long> findByServiceRequestIdAndTransactionType(@Param(value = "reqId") String reqId,
			@Param(value = "transactionType") TransactionType transactionType);

}
