package com.example.demo.contoller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.KafkaConsumerService;

@RestController
@RequestMapping("/admintool")
public class KafkaConsumerController {

	@Autowired
	private KafkaConsumerService kafkaConsumerService;

	@GetMapping("/events")
	public ResponseEntity<List<String>> getMessages() {
		return new ResponseEntity<List<String>>(kafkaConsumerService.getMessages(), HttpStatus.OK);
	}

	@GetMapping("/loggedin/username")
	public ResponseEntity<List<String>> getUsername() {
		return new ResponseEntity<List<String>>(kafkaConsumerService.getLoggedInUsernames(), HttpStatus.OK);

	}

}
