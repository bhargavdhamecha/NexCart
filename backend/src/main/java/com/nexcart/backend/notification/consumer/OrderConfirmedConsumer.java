package com.nexcart.backend.notification.consumer;

import com.nexcart.backend.common.event.KafkaTopics;
import com.nexcart.backend.common.event.OrderConfirmedEvent;
import com.nexcart.backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderConfirmedConsumer {

	private final NotificationService notificationService;

	@KafkaListener(topics = KafkaTopics.ORDER_CONFIRMED, groupId = "notification-service")
	public void onOrderConfirmed(OrderConfirmedEvent event) {
		notificationService.sendOrderConfirmedNotification(event);
	}
}
