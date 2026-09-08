package com.nexcart.backend.common.event;

/** Kafka topic names — context.md's flow: Order -> PaymentRequested, Payment -> PaymentCompleted/
 *  PaymentFailed, Order -> OrderConfirmed, Notification consumes OrderConfirmed. */
public final class KafkaTopics {

	public static final String PAYMENT_REQUESTED = "payment-requested";
	public static final String PAYMENT_COMPLETED = "payment-completed";
	public static final String ORDER_CONFIRMED = "order-confirmed";

	private KafkaTopics() {
	}
}
