package com.example.demo.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.dto.BlogPostDTO;
import com.example.demo.dto.CategoryDTO;
import com.example.demo.dto.ModerationRequest;
import com.example.demo.response.ModerationResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class ModerationService {

	@Value("${moderation.service.url}")
	private String moderationUrl;

	private RestTemplate restTemplate;

	ModerationService(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	Logger logger = LoggerFactory.getLogger(ModerationService.class);


	static  List<String>  HARMFUL_KEYWORDS = List.of(

		    // Violence
		    "kill", "killing", "murder", "murderer", "stab", "stabbing",
		    "shoot", "shooting", "bomb", "explode", "explosive", "attack",
		    "assassinate", "slaughter", "massacre", "behead", "torture",
		    "burn alive", "strangle", "poison", "execute", "lynch","die",

		    // Threats
		    "i will kill you", "i'll kill you", "i am going to kill you",
		    "you deserve to die", "go die", "die now",
		    "i'll stab you", "i'll shoot you",
		    "i will hurt you", "i'll hurt you",
		    "i'll destroy you", "i'll beat you",

		    // Self-harm
		    "suicide", "kill myself", "end my life",
		    "self harm", "self-harm", "cut myself",
		    "hang myself", "overdose", "jump off",
		    "i want to die",

		    // Harassment / Abuse
		    "worthless", "piece of trash", "garbage human",
		    "loser", "moron", "idiot", "dumb", "stupid",
		    "pathetic", "scumbag",

		    // Hate / Extremism
		    "ethnic cleansing", "racial superiority",
		    "genocide", "exterminate",
		    "wipe them out", "kill all",

		    // Illegal activities
		    "make a bomb", "build a bomb",
		    "buy cocaine", "sell cocaine",
		    "buy heroin", "sell heroin",
		    "counterfeit money",
		    "hack bank",
		    "steal credit card",
		    "credit card fraud",
		    "identity theft",
		    "how to poison",
		    "how to make explosives",

		    // Terrorism
		    "terrorist attack",
		    "join terrorist",
		    "bomb school",
		    "bomb airport",
		    "bomb hospital",

		    // Child abuse
		    "child abuse",
		    "sexual abuse of children",
		    "child exploitation"
		);
	
	@Retry(name = "moderationService")
	@CircuitBreaker(name = "moderationService", fallbackMethod = "fallback")
	public ModerationResponse checkContent(BlogPostDTO blogPostDTO) {

		String url = moderationUrl + "/moderate";
		HttpHeaders headers = new HttpHeaders();

		headers.setContentType(MediaType.APPLICATION_JSON);

		List<String> categoriesName = blogPostDTO.getCategories().stream().map(cdto -> cdto.getName()).toList();

		ModerationRequest moderationRequest = new ModerationRequest(blogPostDTO.getTitle(), blogPostDTO.getContent(),
				categoriesName);

		HttpEntity<ModerationRequest> httpEntity = new HttpEntity<ModerationRequest>(moderationRequest, headers);

		ModerationResponse moderationResponse = restTemplate.postForObject(url, httpEntity, ModerationResponse.class);

		return moderationResponse;
	}

	public ModerationResponse fallback(BlogPostDTO blogPostDTO, Exception e) {

		logger.error("Error calling moderation service " + e.getMessage());

		if(e.getMessage().contains("Connection refused")) { // the service is no running
		
			return new ModerationResponse("Moderation service is temporarily unavailable: "+e.getMessage(), false);
		}

		if(checkContentManually(blogPostDTO))
		{
			return new ModerationResponse("rejected", false);
			
		}else {
			return new ModerationResponse("approved", true);
		}
		
	

	}
	
	public boolean checkContentManually(BlogPostDTO blogPostDTO) {


		  String categories = blogPostDTO.getCategories()
		            .stream()
		            .map(CategoryDTO::getName)
		            .collect(Collectors.joining(" "));

		    String data = String.join(" ",
		            blogPostDTO.getTitle(),
		            blogPostDTO.getContent(),
		            categories)
		            .toLowerCase();

		    data = data.replaceAll("[^a-z0-9 ]", " ");
		    data = data.replaceAll("\\s+", " ").trim();

		    return HARMFUL_KEYWORDS.stream()
		            .anyMatch(data::contains);
				
				
				
	}
	

}
