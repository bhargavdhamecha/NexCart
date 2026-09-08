package com.nexcart.backend.common.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Published by OrderServiceImpl.markConfirmed() — consumed by Notification (see
 *  notification.consumer.OrderConfirmedConsumer). */
public record OrderConfirmedEvent(UUID orderId, UUID userId, BigDecimal totalAmount) {
}
