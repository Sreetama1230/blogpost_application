package com.example.demo.clientfallback;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import com.example.demo.client.ModerationClient;
import com.example.demo.dto.BlogPostDTO;
import com.example.demo.dto.CategoryDTO;
import com.example.demo.dto.ModerationRequest;
import com.example.demo.response.ModerationResponse;

import io.github.resilience4j.retry.annotation.Retry;

@Component
public class ModerationClientFallbackFactory implements FallbackFactory<ModerationClient> {

	Logger logger = LoggerFactory.getLogger(ModerationClientFallbackFactory.class);

	@Override
	public ModerationClient create(Throwable e) {
		
		logger.error("Error calling moderation service " + e.getMessage());

		return new ModerationClient() {

			@Override
			public ModerationResponse checkContent(ModerationRequest moderationRequest) {
				
				logger.error("Error calling moderation service " + e.getMessage());

				if (e.getMessage().contains("Connection refused")) { // the service is no running

					return new ModerationResponse("Moderation service is temporarily unavailable: " + e.getMessage(), false);
				}

				if (checkContentManually(moderationRequest)) {
					return new ModerationResponse("rejected", false);

				} else {
					return new ModerationResponse("approved", true);
				}
			}
		};

	}

	static List<String> HARMFUL_KEYWORDS = List.of(

			// Violence
			"kill", "killing", "murder", "murderer", "stab", "stabbing", "shoot", "shooting", "bomb", "explode",
			"explosive", "attack", "assassinate", "slaughter", "massacre", "behead", "torture", "burn alive",
			"strangle", "poison", "execute", "lynch", "die",

			// Threats
			"i will kill you", "i'll kill you", "i am going to kill you", "you deserve to die", "go die", "die now",
			"i'll stab you", "i'll shoot you", "i will hurt you", "i'll hurt you", "i'll destroy you", "i'll beat you",

			// Self-harm
			"suicide", "kill myself", "end my life", "self harm", "self-harm", "cut myself", "hang myself", "overdose",
			"jump off", "i want to die",

			// Harassment / Abuse
			"worthless", "piece of trash", "garbage human", "loser", "moron", "idiot", "dumb", "stupid", "pathetic",
			"scumbag",

			// Hate / Extremism
			"ethnic cleansing", "racial superiority", "genocide", "exterminate", "wipe them out", "kill all",

			// Illegal activities
			"make a bomb", "build a bomb", "buy cocaine", "sell cocaine", "buy heroin", "sell heroin",
			"counterfeit money", "hack bank", "steal credit card", "credit card fraud", "identity theft",
			"how to poison", "how to make explosives",

			// Terrorism
			"terrorist attack", "join terrorist", "bomb school", "bomb airport", "bomb hospital",

			// Child abuse
			"child abuse", "sexual abuse of children", "child exploitation");

	public boolean checkContentManually(ModerationRequest moderationRequest) {

		logger.info("checking the content manually");
		String categories = moderationRequest.getCategories().stream()
				.collect(Collectors.joining(" "));

		String data = String.join(" ", moderationRequest.getTitle(), moderationRequest.getContent(), categories).toLowerCase();

		data = data.replaceAll("[^a-z0-9 ]", " ");
		data = data.replaceAll("\\s+", " ").trim();

		return HARMFUL_KEYWORDS.stream().anyMatch(data::contains);

	}

}
