package com.ai.contentmoderation.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ai.contentmoderation.app.dto.ModerationRequest;
import com.ai.contentmoderation.app.dto.ModerationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ModerationService {

	@Autowired
	private GeminiModerationService geminiModerationService;

    Logger logger = LoggerFactory.getLogger(ModerationService.class);

	public ModerationResponse check(ModerationRequest moderationRequest) {
		
		boolean approved = geminiModerationService.moderate(moderationRequest.getTitle(), moderationRequest.getContent() , moderationRequest.getCategories());
		if(approved) {
            logger.info("Checked with Gemini API and it has been approved");
			return new ModerationResponse("approved", true);
		}else {
            logger.info("Checked with Gemini API and it has been rejected");
			return new ModerationResponse("rejected", false);
		}
	}
	
}
