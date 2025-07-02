package com.example.demo.config;

import com.example.demo.*;
import com.example.demo.constants.AppContants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;

@Configuration
public class KafkaConfig {
	
	Logger logger = LoggerFactory.getLogger(KafkaConfig.class);
	
	
	@KafkaListener(topics = AppContants.ADMINTOOL_TOPIC_NAME, groupId = AppContants.GROUP_ID)
	public void getLoginUserData(String s) {		
		logger.info("username: "+s);
	}
}
