package com.nexcart.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nexcart.backend.common.event.OrderConfirmedEvent;
import com.nexcart.backend.notification.config.NotificationProperties;
import com.nexcart.backend.notification.domain.Notification;
import com.nexcart.backend.notification.domain.NotificationStatus;
import com.nexcart.backend.notification.repository.NotificationRepository;
import com.nexcart.backend.user.dto.UserContact;
import com.nexcart.backend.user.service.UserLookupService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * No REST surface exists for Notification (Kafka-consumer-driven only), so this is a plain
 * Mockito unit test rather than the MockMvc integration-test style used elsewhere.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private UserLookupService userLookupService;

	@Mock
	private JavaMailSender mailSender;

	private NotificationServiceImpl notificationService;

	@BeforeEach
	void setUp() {
		NotificationProperties properties = new NotificationProperties();
		properties.setFromAddress("orders@nexcart.test");
		notificationService = new NotificationServiceImpl(notificationRepository, userLookupService, mailSender,
			properties);
	}

	@Test
	void sendsEmailAndRecordsSentNotificationOnSuccess() {
		UUID userId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		OrderConfirmedEvent event = new OrderConfirmedEvent(orderId, userId, new BigDecimal("49.99"));
		when(userLookupService.getContact(userId))
			.thenReturn(Optional.of(new UserContact("customer@example.com", "Alex")));

		notificationService.sendOrderConfirmedNotification(event);

		ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(mailCaptor.capture());
		assertThat(mailCaptor.getValue().getTo()).containsExactly("customer@example.com");
		assertThat(mailCaptor.getValue().getText()).contains("Alex").contains(orderId.toString());

		ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(notificationCaptor.capture());
		assertThat(notificationCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
		assertThat(notificationCaptor.getValue().getRecipientEmail()).isEqualTo("customer@example.com");
	}

	@Test
	void recordsFailedNotificationWithoutThrowingWhenMailSendFails() {
		UUID userId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		OrderConfirmedEvent event = new OrderConfirmedEvent(orderId, userId, new BigDecimal("49.99"));
		when(userLookupService.getContact(userId))
			.thenReturn(Optional.of(new UserContact("customer@example.com", "Alex")));
		doThrow(new MailAuthenticationException("SMTP not configured")).when(mailSender).send(any(SimpleMailMessage.class));

		// Must not propagate — per context.md, a notification failure must never block order
		// confirmation (which, by the time this runs, has already happened).
		notificationService.sendOrderConfirmedNotification(event);

		ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(notificationCaptor.capture());
		assertThat(notificationCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
		assertThat(notificationCaptor.getValue().getFailureReason()).contains("SMTP not configured");
	}

	@Test
	void doesNothingWhenUserNoLongerExists() {
		UUID userId = UUID.randomUUID();
		OrderConfirmedEvent event = new OrderConfirmedEvent(UUID.randomUUID(), userId, new BigDecimal("10.00"));
		when(userLookupService.getContact(userId)).thenReturn(Optional.empty());

		notificationService.sendOrderConfirmedNotification(event);

		verifyNoInteractions(mailSender);
		verifyNoInteractions(notificationRepository);
	}
}
