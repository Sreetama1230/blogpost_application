package com.ai.contentmoderation.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ai.contentmoderation.app.dto.ModerationRequest;
import com.ai.contentmoderation.app.dto.ModerationResponse;
import com.ai.contentmoderation.app.service.ModerationService;

@RestController
public class ModerationController {

	@Autowired
	private ModerationService moderationService;
	
	@PostMapping("/moderate")
	public ResponseEntity<ModerationResponse> checkContent(@RequestBody ModerationRequest moderationRequest){
		ModerationResponse resp=	moderationService.check(moderationRequest);
		return new ResponseEntity<>(resp , HttpStatus.OK);
	}
}
