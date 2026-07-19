package com.example.demo.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.constants.AppConstants;
import com.example.demo.dao.EventDao;
import com.example.demo.enums.EventStatus;
import com.example.demo.model.Event;

import jakarta.transaction.Transactional;

@Component
@Transactional
public class EventPublisher {

	@Autowired
	EventDao eventDao;
	@Autowired
	KafkaTemplate<String, String> kafkaTemplate;

	Logger logger = LoggerFactory.getLogger(EventPublisher.class);

	@Scheduled(fixedDelay = 5000)
	public void publishEvents() {

		List<Event> events = eventDao.findByStatus(EventStatus.PENDING , 
				PageRequest.of(0,100));
		
		for (Event e : events) {
			e.setStatus(EventStatus.PROCESSING);
			e.setLastAttemptAt(LocalDateTime.now());
			eventDao.save(e);
			
		try {
			
			kafkaTemplate.send(AppConstants.ADMINTOOL_TOPIC_NAME,
						e.toString()).get();
			
			e.setStatus(EventStatus.PUBLISHED);

			e.setPublishedAt(LocalDateTime.now());
			
			eventDao.save(e);
		} catch (InterruptedException | ExecutionException e1) {
		
			   if (e1 instanceof InterruptedException) {
			        Thread.currentThread().interrupt();
			    }
			   
			logger.error("Error occured while publishing the event", e1);
			
			e.setRetryCount(e.getRetryCount() + 1 );
			//retry when fails,  10 times
			if(e.getRetryCount() >= AppConstants.MAX_RETRY_COUNT) {
				e.setStatus(EventStatus.FAILED);
			}else {
				e.setStatus(EventStatus.PENDING);
				
			}
			
		
			e.setLastAttemptAt(LocalDateTime.now());
			eventDao.save(e);
		}

		}

	}
}