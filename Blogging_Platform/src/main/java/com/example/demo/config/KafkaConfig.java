package com.example.demo.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import com.example.demo.constants.AppConstants;

@Configuration
public class KafkaConfig {
	
	@Bean
	public NewTopic topic() {
		return TopicBuilder.name(AppConstants.ADMINTOOL_TOPIC_NAME).build();
	}

}
