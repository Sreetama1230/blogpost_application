package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.constants.AppConstants;
import com.example.demo.response.UserResponse;

@Service
public class KafkaService {

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;
	
	public boolean getLoginUserData(UserResponse userResponse) {
		String username = userResponse.getUsername();
		this.kafkaTemplate.send(AppConstants.ADMINTOOL_TOPIC_NAME,username);
		return true;
	}
	
}
