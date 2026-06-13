package com.example.demo.controller.integration;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

import com.example.demo.dao.UserDao;
import com.example.demo.response.UserResponse;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, 
brokerProperties = 
{ "listeners=PLAINTEXT://localhost:9092",
		"port=9092" })

public class UserControllerIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Autowired
	private UserService userService;

	@Autowired
	private UserDao userDao;

	private static HttpHeaders headers;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeAll
	public static void init() {
		headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
	}

	private String createURLWithPort() {
		return "http://localhost:" + port + "/users";
	}

	@Test
	public void testGetAll() {
		HttpEntity<String> entity = new HttpEntity<>(null, headers);

		ResponseEntity<List<UserResponse>> resp = testRestTemplate.exchange(createURLWithPort(), HttpMethod.GET, entity,
				new ParameterizedTypeReference<List<UserResponse>>() {
				});

		List<UserResponse> userResponses = resp.getBody();

		assertNotNull(userResponses);

	}

	public void testGetById() {

		HttpEntity<String> entity =

				new HttpEntity<String>(null, headers);

		ResponseEntity<UserResponse> resp = testRestTemplate.exchange(createURLWithPort() + "/1001", HttpMethod.GET,
				entity, UserResponse.class);

		UserResponse userResponse = resp.getBody();

	}

}
