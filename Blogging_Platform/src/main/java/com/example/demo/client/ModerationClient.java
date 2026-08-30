package com.example.demo.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.demo.clientfallback.ModerationClientFallbackFactory;
import com.example.demo.dto.ModerationRequest;
import com.example.demo.response.ModerationResponse;

@FeignClient(name="AIContentModerationService" , fallbackFactory = ModerationClientFallbackFactory.class)
public interface ModerationClient {

	   @PostMapping("/moderate")
	   ModerationResponse checkContent(@RequestBody ModerationRequest moderationRequest );

}
