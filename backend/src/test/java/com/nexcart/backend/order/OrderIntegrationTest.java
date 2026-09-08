package com.nexcart.backend.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.nexcart.backend.common.event.PaymentRequestedEvent;
import com.nexcart.backend.inventory.domain.Inventory;
import com.nexcart.backend.inventory.repository.InventoryRepository;
import com.nexcart.backend.order.repository.OrderItemRepository;
import com.nexcart.backend.order.repository.OrderRepository;
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
class OrderIntegrationTest {

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
	private FakeEventPublisherImpl eventPublisher;

	@Autowired
	private WebApplicationContext webApplicationContext;

	private UUID productId;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
			.apply(SecurityMockMvcConfigurers.springSecurity())
			.build();
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
	void checkoutCreatesOrderFromCartDecrementsInventoryAndLeavesCartUntouched() throws Exception {
		String token = createUserAndLogin();
		addToCart(token, productId, 2);

		MvcResult result = mockMvc.perform(post("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("CREATED"))
			.andExpect(jsonPath("$.items[0].title").value("Test Widget"))
			.andExpect(jsonPath("$.items[0].unitPrice").value(19.99))
			.andExpect(jsonPath("$.items[0].itemTotal").value(39.98))
			.andReturn();

		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
		assertThat(body.get("totalAmount").asDouble()).isEqualTo(39.98);

		assertThat(inventoryRepository.findByProductId(productId).orElseThrow().getAvailableQuantity()).isEqualTo(3);

		// Cart is deliberately left populated at checkout time — it's only cleared once payment
		// actually succeeds (see PaymentIntegrationTest), so a declined/abandoned payment doesn't
		// leave the user with an empty cart for items they never actually bought.
		mockMvc.perform(get("/api/v1/cart").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].quantity").value(2));

		assertThat(eventPublisher.events()).anySatisfy(published -> {
			assertThat(published.topic()).isEqualTo(KafkaTopics.PAYMENT_REQUESTED);
			assertThat(published.payload()).isInstanceOf(PaymentRequestedEvent.class);
			PaymentRequestedEvent event = (PaymentRequestedEvent) published.payload();
			assertThat(event.amount()).isEqualByComparingTo("39.98");
		});
	}

	@Test
	void checkoutWithEmptyCartReturns400() throws Exception {
		String token = createUserAndLogin();

		mockMvc.perform(post("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("CART_EMPTY"));
	}

	@Test
	void checkoutWithInactiveProductRejectsAndDoesNotDecrementInventory() throws Exception {
		String token = createUserAndLogin();
		addToCart(token, productId, 1);

		// Deactivate via the real admin API (DELETE = soft-delete to INACTIVE), not a direct
		// repository write — the product catalog cache is only ever invalidated through
		// ProductServiceImpl's own mutating methods (see its @CacheEvict annotations), so a
		// write that bypasses the service layer wouldn't be reflected in what checkout() reads
		// back, same as it wouldn't reach real callers of the API either.
		deactivateProduct(productId);

		mockMvc.perform(post("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PRODUCT_UNAVAILABLE"));

		assertThat(inventoryRepository.findByProductId(productId).orElseThrow().getAvailableQuantity()).isEqualTo(5);
		assertThat(orderRepository.count()).isEqualTo(0);
	}

	@Test
	void checkoutWithInsufficientStockRejectsAndDoesNotDecrementInventory() throws Exception {
		String token = createUserAndLogin();
		addToCart(token, productId, 999);

		mockMvc.perform(post("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

		assertThat(inventoryRepository.findByProductId(productId).orElseThrow().getAvailableQuantity()).isEqualTo(5);
		assertThat(orderRepository.count()).isEqualTo(0);
	}

	@Test
	void getOrdersListsOnlyOwnOrders() throws Exception {
		String tokenA = createUserAndLogin();
		String tokenB = createUserAndLogin();
		addToCart(tokenA, productId, 1);
		checkout(tokenA);

		mockMvc.perform(get("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1));

		mockMvc.perform(get("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void getOrderByIdForAnotherUsersOrderReturns404() throws Exception {
		String tokenA = createUserAndLogin();
		String tokenB = createUserAndLogin();
		addToCart(tokenA, productId, 1);
		JsonNode order = checkout(tokenA);

		mockMvc.perform(get("/api/v1/orders/" + order.get("orderId").asText())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
	}

	@Test
	void anonymousCheckoutIsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/orders"))
			.andExpect(status().isUnauthorized());
	}

	private JsonNode checkout(String token) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isCreated())
			.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private void addToCart(String token, UUID productId, int quantity) throws Exception {
		mockMvc.perform(post("/api/v1/cart/items")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("productId", productId.toString(), "quantity", quantity))))
			.andExpect(status().isOk());
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

	private void deactivateProduct(UUID productId) throws Exception {
		String email = "admin-" + UUID.randomUUID() + "@example.com";
		User admin = User.create("Admin", "User", email, passwordEncoder.encode("Password1"));
		admin.setRole(UserRole.ADMIN);
		userRepository.save(admin);

		MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("email", email, "password", "Password1"))))
			.andExpect(status().isOk())
			.andReturn();
		String adminToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
			.get("accessToken").asText();

		mockMvc.perform(delete("/api/v1/products/" + productId).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
			.andExpect(status().isNoContent());
	}

	private String json(Object value) throws Exception {
		return objectMapper.writeValueAsString(value);
	}
}
