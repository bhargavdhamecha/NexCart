package com.nexcart.backend.notification.service;

import com.nexcart.backend.common.event.OrderConfirmedEvent;

public interface NotificationService {

	/**
	 * Sends an order-confirmation email and records the attempt. Never throws — a send failure
	 * (e.g. mail not configured, provider rejects it) is recorded as a FAILED Notification row
	 * and swallowed, per context.md: "Notification failure must not block order confirmation."
	 * By the time this runs (async, via Kafka), the order is already confirmed regardless.
	 */
	void sendOrderConfirmedNotification(OrderConfirmedEvent event);
}
