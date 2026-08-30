package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.client.ModerationClient;
import com.example.demo.dto.ModerationRequest;
import com.example.demo.response.ModerationResponse;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class ModerationServiceClient {

	@Autowired
	private ModerationClient moderationClient;

	@Retry(name = "AIContentModerationService")
	public
	ModerationResponse checkContent(ModerationRequest moderationRequest) {

		return moderationClient.checkContent(moderationRequest);

	}

}
