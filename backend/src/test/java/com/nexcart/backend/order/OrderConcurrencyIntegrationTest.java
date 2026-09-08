package com.nexcart.backend.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.backend.cart.repository.CartItemRepository;
import com.nexcart.backend.cart.repository.CartRepository;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

/**
 * Proves InventoryRepository's pessimistic write lock actually prevents overselling under real
 * concurrent checkouts — context.md's own callout scenario: "stock = 1, multiple users checkout
 * concurrently, only one purchase should succeed."
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderConcurrencyIntegrationTest {

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
	private WebApplicationContext webApplicationContext;

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
	}

	@Test
	void concurrentCheckoutForLastUnitOnlyOneSucceeds() throws Exception {
		Category category = categoryRepository.save(Category.create("Electronics"));
		Product product = productRepository.save(Product.create("Last Unit Item", "Only one left",
			new BigDecimal("9.99"), category));
		UUID productId = product.getId();
		inventoryRepository.save(Inventory.initialize(productId, 1));

		String tokenA = createUserAndLogin();
		String tokenB = createUserAndLogin();
		addToCart(tokenA, productId, 1);
		addToCart(tokenB, productId, 1);

		CountDownLatch bothReady = new CountDownLatch(2);
		CountDownLatch go = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);

		try {
			Callable<Integer> checkoutA = checkoutTask(tokenA, bothReady, go);
			Callable<Integer> checkoutB = checkoutTask(tokenB, bothReady, go);
			Future<Integer> resultA = executor.submit(checkoutA);
			Future<Integer> resultB = executor.submit(checkoutB);

			bothReady.await(5, TimeUnit.SECONDS);
			go.countDown(); // release both threads at (as close to) the same instant as possible

			int statusA = resultA.get(15, TimeUnit.SECONDS);
			int statusB = resultB.get(15, TimeUnit.SECONDS);

			List<Integer> statuses = List.of(statusA, statusB);
			// The correctness property that actually matters — exactly one checkout wins,
			// regardless of exactly which non-2xx status the loser gets back.
			assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
			assertThat(statuses).filteredOn(status -> status != 201).hasSize(1);
		}
		finally {
			executor.shutdown();
		}

		assertThat(orderRepository.count()).isEqualTo(1);
		assertThat(inventoryRepository.findByProductId(productId).orElseThrow().getAvailableQuantity()).isEqualTo(0);
	}

	private Callable<Integer> checkoutTask(String token, CountDownLatch bothReady, CountDownLatch go) {
		return () -> {
			bothReady.countDown();
			go.await();
			MvcResult result = mockMvc.perform(post("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andReturn();
			return result.getResponse().getStatus();
		};
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

	private String json(Object value) throws Exception {
		return objectMapper.writeValueAsString(value);
	}
}
