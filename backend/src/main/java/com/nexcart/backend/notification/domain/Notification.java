package com.nexcart.backend.notification.domain;

import com.nexcart.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Column(nullable = false, length = 255)
	private String recipientEmail;

	@Column(nullable = false, length = 255)
	private String subject;

	@Column(nullable = false, length = 2000)
	private String message;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NotificationStatus status;

	// Nullable — only set when status = FAILED (e.g. SMTP not configured, provider rejected it).
	@Column(length = 1000)
	private String failureReason;

	public static Notification create(UUID userId, UUID orderId, String recipientEmail, String subject,
		String message, NotificationStatus status, String failureReason) {
		Notification notification = new Notification();
		notification.userId = userId;
		notification.orderId = orderId;
		notification.recipientEmail = recipientEmail;
		notification.subject = subject;
		notification.message = message;
		notification.status = status;
		notification.failureReason = failureReason;
		return notification;
	}
}
