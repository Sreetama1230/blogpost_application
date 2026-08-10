package com.notificationservice.app.service;


import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.notificationservice.app.constant.AppConstants;
import com.notificationservice.app.constant.NotificationStringContants;

@Service
public class KafkaConsumerService {

	Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

	public List<String> list = new CopyOnWriteArrayList<>();

	@KafkaListener(topics = AppConstants.NOTIFICATION_TOPIC, groupId = AppConstants.GROUP_ID)
	public void getConsumerMessages(String message) {

		String[] str = message.split(" ");

		if (str[0].equals("USER")) {

			switch (str[1]) {
			case "UPDATE":
				list.add(NotificationStringContants.USER_UPDATED);
				break;
			case "DELETE":
				list.add(NotificationStringContants.USER_DELETED);
				break;

			case "FOLLOW":
				list.add(NotificationStringContants.USER_FOLLOW);
				break;

			}
		}

		if (str[0].equals("BLOGPOST")) {
			switch (str[1]) {
			case "CREATE":
				list.add(NotificationStringContants.BlOGPOST_CREATED);
				break;
			case "UPDATE":
				list.add(NotificationStringContants.BlOGPOST_UPDATED);
				break;
			case "DELETE":
				list.add(NotificationStringContants.BLOGPOST_DELETED);
				break;

			case "LIKE":
				list.add(NotificationStringContants.BLOGPOST_REACTED_LIKE);
				break;
			case "PIN":
				list.add(NotificationStringContants.BLOGPOST_PIN);
				break;
			}
		}

		if (str[0].equals("COMMENT")) {
			switch (str[1]) {
			case "CREATE":
				list.add(NotificationStringContants.COMMENT_ADDED);
				break;
			case "UPDATE":
				list.add(NotificationStringContants.COMMENT_UPDATED);
				break;
			case "DELETE":
				list.add(NotificationStringContants.COMMENT_DELETED);
				break;

			case "REACT":
				list.add(NotificationStringContants.COMMENT_REACTED);
				break;

			}
		}

		if (str[0].equals("CATEGORY")) {
			switch (str[1]) {
			case "CREATE":
				list.add(NotificationStringContants.CATEGORY_ADDED);
				break;
			case "DELETE":
				list.add(NotificationStringContants.CATEGORY_DELETED);
				break;

			}
		}
//event.getTransactionType() + " " + event.getEventType() + " " + event.getRecipientUserId()+" "+event.getActorUserId())

//		notificationRepository
//				.save(new Notification(Long.valueOf(str[2]), Long.valueOf(str[3]), message, LocalDateTime.now()));

	}

	public List<String> getMessages() {

		return list;

	}

}
