package com.nexcart.backend.payment.controller;

import com.nexcart.backend.payment.dto.PaymentInitiateRequest;
import com.nexcart.backend.payment.dto.PaymentSummary;
import com.nexcart.backend.payment.dto.PaymentVerifyRequest;
import com.nexcart.backend.payment.service.PaymentService;
import com.nexcart.backend.security.jwt.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;

	@PostMapping("/initiate")
	public PaymentSummary initiate(@AuthenticationPrincipal AuthenticatedUser user,
		@Valid @RequestBody PaymentInitiateRequest request) {
		return paymentService.initiate(user.id(), request);
	}

	@PostMapping("/{id}/verify")
	public PaymentSummary verify(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
		@Valid @RequestBody PaymentVerifyRequest request) {
		return paymentService.verify(user.id(), id, request);
	}

	@GetMapping("/{id}")
	public PaymentSummary getPayment(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		return paymentService.getPayment(user.id(), id);
	}
}
