package com.nexcart.backend.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.backend.cart.repository.CartItemRepository;
import com.nexcart.backend.cart.repository.CartRepository;
import com.nexcart.backend.common.event.FakeEventPublisherImpl;
import com.nexcart.backend.common.event.KafkaTopics;
import com.nexcart.backend.common.event.OrderConfirmedEvent;
import com.nexcart.backend.common.event.PaymentCompletedEvent;
import com.nexcart.backend.inventory.domain.Inventory;
import com.nexcart.backend.inventory.repository.InventoryRepository;
import com.nexcart.backend.order.repository.OrderItemRepository;
import com.nexcart.backend.order.repository.OrderRepository;
import com.nexcart.backend.payment.gateway.FakeRazorpayGatewayClientImpl;
import com.nexcart.backend.payment.repository.PaymentRepository;
import com.nexcart.backend.product.domain.Category;
import com.nexcart.backend.product.domain.Product;
import com.nexcart.backend.product.repository.CategoryRepository;
import com.nexcart.backend.product.repository.ProductImageRepository;
import com.nexcart.backend.product.repository.ProductRepository;
import com.nexcart.backend.user.domain.User;
import com.nexcart.backend.user.domain.UserRole;
import com.nexcart.backend.user.repository.RefreshSessionRepository;
import com.nexcart.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class PaymentIntegrationTest {

	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshSessionRepository refreshSessionRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private InventoryRepository inventoryRepository;

	@Autowired
	private ProductImageRepository productImageRepository;

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private CartItemRepository cartItemRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderItemRepository orderItemRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private FakeEventPublisherImpl eventPublisher;

	@Autowired
	private WebApplicationContext webApplicationContext;

	private UUID productId;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
			.apply(SecurityMockMvcConfigurers.springSecurity())
			.build();
		paymentRepository.deleteAll();
		orderItemRepository.deleteAll();
		orderRepository.deleteAll();
		cartItemRepository.deleteAll();
		cartRepository.deleteAll();
		productImageRepository.deleteAll();
		inventoryRepository.deleteAll();
		productRepository.deleteAll();
		categoryRepository.deleteAll();
		refreshSessionRepository.deleteAll();
		userRepository.deleteAll();
		eventPublisher.clear();

		Category category = categoryRepository.save(Category.create("Electronics"));
		Product product = productRepository.save(Product.create("Test Widget", "A widget",
			new BigDecimal("19.99"), category));
		productId = product.getId();
		inventoryRepository.save(Inventory.initialize(productId, 5));
	}

	@Test
	void initiatePaymentCreatesInitiatedPaymentAndSetsOrderPendingPayment() throws Exception {
		String token = createUserAndLogin();
		String orderId = checkoutOrder(token, 1);

		mockMvc.perform(post("/api/v1/payments/initiate")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("orderId", orderId, "idempotencyKey", "key-1", "paymentMethod", "RAZORPAY"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("INITIATED"));

		mockMvc.perform(get("/api/v1/orders/" + orderId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
	}

	@Test
	void initiateReturnsRazorpayOrderIdAndKeyId() throws Exception {
		String token = createUserAndLogin();
		String orderId = checkoutOrder(token, 1);

		JsonNode payment = initiate(token, orderId, "key-rzp-fields");

		assertThat(payment.get("razorpayOrderId").asText()).isNotBlank();
		assertThat(payment.get("razorpayKeyId").asText()).isEqualTo("rzp_test_fake_key_id_for_tests");
	}

	@Test
	void initiatingTwiceWithSameIdempotencyKeyReturnsSamePayment() throws Exception {
		String token = createUserAndLogin();
		String orderId = checkoutOrder(token, 1);

		JsonNode first = initiate(token, orderId, "key-dup");
		JsonNode second = initiate(token, orderId, "key-dup");

		assertThat(first.get("paymentId").asText()).isEqualTo(second.get("paymentId").asText());
		assertThat(paymentRepository.count()).isEqualTo(1);
	}

	@Test
	void initiateForAnotherUsersOrderReturns404() throws Exception {
		String tokenA = createUserAndLogin();
		String tokenB = createUserAndLogin();
		String orderId = checkoutOrder(tokenA, 1);

		mockMvc.perform(post("/api/v1/payments/initiate")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("orderId", orderId, "idempotencyKey", "key-b", "paymentMethod", "RAZORPAY"))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
	}

	@Test
	void initiatingForSameOrderFromTwoAttemptsReusesThePendingPayment() throws Exception {
		String token = createUserAndLogin();
		String orderId = checkoutOrder(token, 1);

		// Two different idempotency keys, simulating two browser tabs both clicking Pay.
		JsonNode first = initiate(token, orderId, "key-tab-a");
		JsonNode second = initiate(token, orderId, "key-tab-b");

		assertThat(second.get("paymentId").asText()).isEqualTo(first.get("paymentId").asText());
		assertThat(second.get("razorpayOrderId").asText()).isEqualTo(first.get("razorpayOrderId").asText());
		assertThat(paymentRepository.count()).isEqualTo(1);
	}

	@Test
	void initiateForNonPayableOrderReturns409() throws Exception {
		String token = createUserAndLogin();
		String orderId = checkoutOrder(token, 1);
		JsonNode payment = initiate(token, orderId, "key-confirm");
		verify(token, payment);

		mockMvc.perform(post("/api/v1/payments/initiate")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("orderId", orderId, "idempotencyKey", "key-after-confirm",
					"paymentMethod", "RAZORPAY"))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INVALID_ORDER_STATE"));
	}

	@Test
	void verifyWithValidSignatureConfirmsOrderAndSetsTransactionId() throws Exception {
		String token = createUserAndLogin();
		String orderId = checkoutOrder(token, 1);
		JsonNode payment = initiate(token, orderId, "key-success");

		JsonNode verified = verify(token, payment);

		assertThat(verified.get("status").asText()).isEqualTo("SUCCESS");
		assertThat(verified.get("transactionId").asText()).isNotBlank();

		mockMvc.perform(get("/api/v1/orders/" + orderId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(jsonPath("$.status").value("CONFIRMED"));

		// Cart was deliberately left populated at checkout time — a successful payment is the
		// one place it actually gets cleared.
		mockMvc.perform(get("/api/v1/cart").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(jsonPath("$.items.length()").value(0));

		assertThat(eventPublisher.events()).anySatisfy(published -> {
			assertThat(published.topic()).isEqualTo(KafkaTopics.PAYMENT_COMPLETED);
			assertThat(((PaymentCompletedEvent) published.payload()).success()).isTrue();
		});
		assertThat(eventPublisher.events()).anySatisfy(published -> {
			assertThat(published.topic()).isEqualTo(KafkaTopics.ORDER_CONFIRMED);
			assertThat(((OrderConfirmedEvent) published.payload()).orderId()).isEqualTo(UUID.fromString(orderId));
		});
	}

	@Test
	void verifyWithInvalidSignatureReturns400AndLeavesStateUntouched() throws Exception {
		String token = createUserAndLogin();
		String orderId = checkoutOrder(token, 1);
		JsonNode payment = initiate(token, orderId, "key-tampered");

		mockMvc.perform(post("/api/v1/payments/" + payment.get("paymentId").asText() + "/verify")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("razorpayPaymentId", "pay_fake_tampered", "razorpaySignature", "not-a-real-signature"))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("PAYMENT_SIGNATURE_INVALID"));

		mockMvc.perform(get("/api/v1/orders/" + orderId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
		mockMvc.perform(get("/api/v1/payments/" + payment.get("paymentId").asText())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(jsonPath("$.status").value("INITIATED"));
		mockMvc.perform(get("/api/v1/cart").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(jsonPath("$.items.length()").value(1));
	}

	@Test
	void verifyingAlreadyTerminalPaymentIsIdempotent() throws Exception {
		String token = createUserAndLogin();
		String orderId = checkoutOrder(token, 1);
		JsonNode payment = initiate(token, orderId, "key-idempotent");

		JsonNode first = verify(token, payment);
		// A second verify call for the same payment, even with a different (but validly-signed
		// for a different fake payment id) request, must still just return the first result.
		String razorpayOrderId = payment.get("razorpayOrderId").asText();
		String otherPaymentId = "pay_fake_should_be_ignored";
		String otherSignature = FakeRazorpayGatewayClientImpl.computeSignature(razorpayOrderId, otherPaymentId);
		MvcResult secondResult = mockMvc.perform(post("/api/v1/payments/" + payment.get("paymentId").asText() + "/verify")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("razorpayPaymentId", otherPaymentId, "razorpaySignature", otherSignature))))
			.andExpect(status().isOk())
			.andReturn();
		JsonNode second = objectMapper.readTree(secondResult.getResponse().getContentAsString());

		assertThat(second.get("status").asText()).isEqualTo("SUCCESS");
		assertThat(second.get("transactionId").asText()).isEqualTo(first.get("transactionId").asText());

		mockMvc.perform(get("/api/v1/orders/" + orderId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(jsonPath("$.status").value("CONFIRMED"));
	}

	@Test
	void verifyForAnotherUsersPaymentReturns404() throws Exception {
		String tokenA = createUserAndLogin();
		String tokenB = createUserAndLogin();
		String orderId = checkoutOrder(tokenA, 1);
		JsonNode payment = initiate(tokenA, orderId, "key-owner");

		mockMvc.perform(post("/api/v1/payments/" + payment.get("paymentId").asText() + "/verify")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("razorpayPaymentId", "pay_fake_x", "razorpaySignature", "irrelevant"))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
	}

	@Test
	void anonymousInitiateIsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/payments/initiate")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("orderId", UUID.randomUUID().toString(), "idempotencyKey", "x",
					"paymentMethod", "RAZORPAY"))))
			.andExpect(status().isUnauthorized());
	}

	private JsonNode initiate(String token, String orderId, String idempotencyKey) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/payments/initiate")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("orderId", orderId, "idempotencyKey", idempotencyKey,
					"paymentMethod", "RAZORPAY"))))
			.andExpect(status().isOk())
			.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	/** Verifies with a genuinely-valid (fake-gateway-computed) signature for the given payment. */
	private JsonNode verify(String token, JsonNode payment) throws Exception {
		String razorpayOrderId = payment.get("razorpayOrderId").asText();
		String razorpayPaymentId = "pay_fake_" + UUID.randomUUID();
		String signature = FakeRazorpayGatewayClientImpl.computeSignature(razorpayOrderId, razorpayPaymentId);

		MvcResult result = mockMvc.perform(post("/api/v1/payments/" + payment.get("paymentId").asText() + "/verify")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("razorpayPaymentId", razorpayPaymentId, "razorpaySignature", signature))))
			.andExpect(status().isOk())
			.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private String checkoutOrder(String token, int quantity) throws Exception {
		mockMvc.perform(post("/api/v1/cart/items")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("productId", productId.toString(), "quantity", quantity))))
			.andExpect(status().isOk());

		MvcResult result = mockMvc.perform(post("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isCreated())
			.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("orderId").asText();
	}

	private String createUserAndLogin() throws Exception {
		String email = "user-" + UUID.randomUUID() + "@example.com";
		User user = User.create("Test", "User", email, passwordEncoder.encode("Password1"));
		user.setRole(UserRole.CUSTOMER);
		userRepository.save(user);

		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("email", email, "password", "Password1"))))
			.andExpect(status().isOk())
			.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
	}

	private String json(Object value) throws Exception {
		return objectMapper.writeValueAsString(value);
	}
}
