package com.nexcart.backend.notification.service;

import com.nexcart.backend.common.event.OrderConfirmedEvent;
import com.nexcart.backend.notification.config.NotificationProperties;
import com.nexcart.backend.notification.domain.Notification;
import com.nexcart.backend.notification.domain.NotificationStatus;
import com.nexcart.backend.notification.repository.NotificationRepository;
import com.nexcart.backend.user.dto.UserContact;
import com.nexcart.backend.user.service.UserLookupService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserLookupService userLookupService;
	private final JavaMailSender mailSender;
	private final NotificationProperties notificationProperties;

	@Override
	@Transactional
	public void sendOrderConfirmedNotification(OrderConfirmedEvent event) {
		Optional<UserContact> contact = userLookupService.getContact(event.userId());
		if (contact.isEmpty()) {
			log.warn("No user found for order-confirmed notification: order={}, user={}", event.orderId(),
				event.userId());
			return;
		}

		String subject = "Your NexCart order is confirmed";
		String message = "Hi " + contact.get().firstName() + ",\n\n"
			+ "Your order " + event.orderId() + " has been confirmed.\n"
			+ "Total: " + event.totalAmount() + "\n\n"
			+ "Thanks for shopping with NexCart!";

		try {
			SimpleMailMessage mail = new SimpleMailMessage();
			mail.setFrom(notificationProperties.getFromAddress());
			mail.setTo(contact.get().email());
			mail.setSubject(subject);
			mail.setText(message);
			mailSender.send(mail);

			notificationRepository.save(Notification.create(event.userId(), event.orderId(), contact.get().email(),
				subject, message, NotificationStatus.SENT, null));
		}
		catch (MailException exception) {
			log.warn("Failed to send order-confirmed email for order {}", event.orderId(), exception);
			notificationRepository.save(Notification.create(event.userId(), event.orderId(), contact.get().email(),
				subject, message, NotificationStatus.FAILED, exception.getMessage()));
		}
	}
}
