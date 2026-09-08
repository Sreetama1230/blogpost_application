package com.example.demo.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.demo.constants.AppConstants;

@Service
public class KafkaConsumerService {

	Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

	private final List<String> messages = new CopyOnWriteArrayList<>();
	private final List<String> usernames = new CopyOnWriteArrayList<>();

	@KafkaListener(topics = AppConstants.ADMINTOOL_EVENTS_TOPIC, groupId = AppConstants.GROUP_ID)
	public void getConsumedMessages(String s) {
		messages.add(s);

	}

	@KafkaListener(topics = AppConstants.ADMINTOOL_USERNAME_TOPIC, groupId = AppConstants.GROUP_ID)
	public void getLoggedInUserName(String s) {
		logger.info("Currently logged in username: " + s);
		usernames.add(s);

	}

	
	public List<String> getMessages() {
		return messages;
	}

	public List<String> getLoggedInUsernames() {
		return usernames;
	}
}
