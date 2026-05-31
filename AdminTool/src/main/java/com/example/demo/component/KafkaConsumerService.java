package com.example.demo.component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.demo.constants.AppContants;

@Service
public class KafkaConsumerService {

	Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

	private final List<String> messages = new CopyOnWriteArrayList<>();

	@KafkaListener(topics = AppContants.ADMINTOOL_TOPIC_NAME, groupId = AppContants.GROUP_ID)
	public void getConsumedMessages(String s) {
		logger.info("username: " + s);
		messages.add(s);

	}

	public List<String> getMessages() {
		return messages;
	}

}
