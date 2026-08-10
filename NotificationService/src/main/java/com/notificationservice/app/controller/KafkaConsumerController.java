package com.notificationservice.app.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.notificationservice.app.service.KafkaConsumerService;



@RestController
@RequestMapping("/notification")
public class KafkaConsumerController {

	
	@Autowired
	private KafkaConsumerService kafkaConsumerService;
	@GetMapping
	public List<String> getMessages(){
		return kafkaConsumerService.getMessages();
	}
}
