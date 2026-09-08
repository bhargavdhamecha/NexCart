package com.nexcart.backend.common.event;

import java.util.UUID;

/** Published by PaymentServiceImpl.complete() — covers both PaymentCompleted and PaymentFailed
 *  from context.md's flow (distinguished by the success flag rather than two event types). */
public record PaymentCompletedEvent(UUID orderId, UUID paymentId, UUID userId, boolean success, String transactionId) {
}
