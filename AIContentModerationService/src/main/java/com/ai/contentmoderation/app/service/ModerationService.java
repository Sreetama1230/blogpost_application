package com.ai.contentmoderation.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ai.contentmoderation.app.dto.ModerationRequest;
import com.ai.contentmoderation.app.dto.ModerationResponse;

@Service
public class ModerationService {

	@Autowired
	private GeminiModerationService geminiModerationService;
	
	
	public ModerationResponse check(ModerationRequest moderationRequest) {
		
		boolean approved = geminiModerationService.moderate(moderationRequest.getTitle(), moderationRequest.getContent() , moderationRequest.getCategories());
		if(approved) {
			return new ModerationResponse("approved", true);
		}else {
			return new ModerationResponse("rejected", false);
		}
	}
	
}
