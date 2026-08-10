package com.ai.contentmoderation.app.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ai.contentmoderation.app.dto.Content;
import com.ai.contentmoderation.app.dto.GeminiRequest;
import com.ai.contentmoderation.app.dto.GeminiResponse;
import com.ai.contentmoderation.app.dto.Part;

@Service
public class GeminiModerationService {

	@Value("${gemini.api.key}")
	private String apiKey;

	@Autowired
	private RestTemplate restTemplate;

	public boolean moderate(String title, String desc, List<String> categories) {

			String categoryText = String.join(",", categories);
		
		String prompt = """
							You are an expert Content Moderator. Your task is to analyze the provided "Title", "Content", "Categories" text for policy violations.

				### Safety Policies
				1. Hate Speech: Content that attacks, denigrates, or incites hatred against individuals or groups based on protected characteristics (race, ethnicity, religion, gender, sexual orientation, disability, etc.).
				2. Harassment and Bullying: Content that targets individuals with abusive, insulting, intimidating, or demeaning language.
				3. Dangerous Expression: Content that promotes, encourages, or instructs on illegal acts, physical violence, self-harm, or severe harm to people or property.
				4. Destructive Vocabulary: Content that uses malicious, toxic, or highly abusive words intended to cause psychological distress or damage.

				### Evaluation Rules
				- Check the Title, Content and Categories independently.
				- If ANY section violates one or more of the Safety Policies, the entire submission must be flagged as "Unsafe" i.e. "REJECTED".
				- Maintain a strict, neutral standard. Do not allow subtle or masked violations (e.g., coded language or dog whistles).

								            Return ONLY one word.

								            APPROVED

								            or

								            REJECTED

								            Blog Title:
								            %s

								            Blog Content:
								            %s
								            
								            Categories:
								            %s
								"""
				.formatted(title, desc, categoryText);
		

		GeminiRequest geminiRequest = new GeminiRequest(List.of(new Content(List.of(new Part(prompt)))));

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);

		// Represents an HTTP request or response entity, consisting of headers and
		// body.
		HttpEntity<GeminiRequest> httpEntity = new HttpEntity<GeminiRequest>(geminiRequest, httpHeaders);
		String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key="
				+ apiKey;

		GeminiResponse geminiResponse = restTemplate.postForObject(url, httpEntity, GeminiResponse.class);

		String resp = geminiResponse.getCandidates().get(0).getContent().getParts().get(0).getText();

		return resp.equalsIgnoreCase("APPROVED");

	}

}
