package com.example.demo.kafkaservice;

import java.time.LocalDateTime;

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
	
	Logger logger = LoggerFactory.getLogger(KafkaService.class);
	public boolean getLoginUserData(UserResponse userResponse) {
		String username = userResponse.getUsername();
		kafkaTemplate.send(
		        AppConstants.ADMINTOOL_USERNAME_TOPIC,
		        "Currently logged in username: " + username +" TimeStamp : "+LocalDateTime.now()
		).whenComplete((result, ex) -> {

		    if (ex != null) {
		        logger.error("Failed to send Kafka message", ex);
		    } else {
		        logger.info(
		            "Message sent successfully. Topic={}, Partition={}, Offset={}",
		            result.getRecordMetadata().topic(),
		            result.getRecordMetadata().partition(),
		            result.getRecordMetadata().offset()
		        );
		    }
		});
		return true;
	}
	
}
