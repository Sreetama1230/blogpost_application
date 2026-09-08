package com.example.demo.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import com.example.demo.constants.AppConstants;

@Configuration
public class KafkaConfig {
	
	@Bean
	public NewTopic topicAdminToolEvents() {
		return TopicBuilder.name(AppConstants.ADMINTOOL_EVENTS_TOPIC).build();
	}
	
	@Bean
	public NewTopic topicAdminTool() {
		return TopicBuilder.name(AppConstants.ADMINTOOL_USERNAME_TOPIC).build();
	}
	
	@Bean
	public NewTopic topicNotification() {
		return TopicBuilder.name(AppConstants.NOTIFICATION_TOPIC).build();
	}
	

}
