package com.nexcart.backend.common.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Published by OrderServiceImpl.checkout() once an order is created and ready for payment. */
public record PaymentRequestedEvent(UUID orderId, UUID userId, BigDecimal amount) {
}
