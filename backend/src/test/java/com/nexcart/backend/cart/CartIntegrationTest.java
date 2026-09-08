package com.nexcart.backend.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.backend.cart.repository.CartItemRepository;
import com.nexcart.backend.cart.repository.CartRepository;
import com.nexcart.backend.inventory.domain.Inventory;
import com.nexcart.backend.inventory.repository.InventoryRepository;
import com.nexcart.backend.product.domain.Category;
import com.nexcart.backend.product.domain.Product;
import com.nexcart.backend.product.domain.ProductStatus;
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
class CartIntegrationTest {

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
	private WebApplicationContext webApplicationContext;

	private UUID productId;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
			.apply(SecurityMockMvcConfigurers.springSecurity())
			.build();
		cartItemRepository.deleteAll();
		cartRepository.deleteAll();
		productImageRepository.deleteAll();
		inventoryRepository.deleteAll();
		productRepository.deleteAll();
		categoryRepository.deleteAll();
		refreshSessionRepository.deleteAll();
		userRepository.deleteAll();

		Category category = categoryRepository.save(Category.create("Electronics"));
		Product product = productRepository.save(Product.create("Test Widget", "A widget",
			new BigDecimal("19.99"), category));
		productId = product.getId();
		inventoryRepository.save(Inventory.initialize(productId, 20));
	}

	@Test
	void addItemToCartSucceeds() throws Exception {
		String token = createUserAndLogin();

		MvcResult result = mockMvc.perform(post("/api/v1/cart/items")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("productId", productId.toString(), "quantity", 2))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].title").value("Test Widget"))
			.andExpect(jsonPath("$.items[0].quantity").value(2))
			.andExpect(jsonPath("$.itemCount").value(2))
			.andReturn();

		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
		assertThat(body.get("totalAmount").asDouble()).isEqualTo(39.98);
	}

	@Test
	void addItemTwiceIncrementsQuantity() throws Exception {
		String token = createUserAndLogin();

		addItem(token, productId, 2);
		addItem(token, productId, 3);

		mockMvc.perform(get("/api/v1/cart").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].quantity").value(5));
	}

	@Test
	void setItemQuantitySucceeds() throws Exception {
		String token = createUserAndLogin();
		addItem(token, productId, 1);

		mockMvc.perform(put("/api/v1/cart/items/" + productId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("quantity", 10))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].quantity").value(10));
	}

	@Test
	void setItemQuantityForItemNotInCartReturns404() throws Exception {
		String token = createUserAndLogin();

		mockMvc.perform(put("/api/v1/cart/items/" + productId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("quantity", 10))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("CART_ITEM_NOT_FOUND"));
	}

	@Test
	void removeItemSucceeds() throws Exception {
		String token = createUserAndLogin();
		addItem(token, productId, 1);

		mockMvc.perform(delete("/api/v1/cart/items/" + productId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items.length()").value(0));
	}

	@Test
	void removeItemNotInCartIsIdempotent() throws Exception {
		String token = createUserAndLogin();

		mockMvc.perform(delete("/api/v1/cart/items/" + productId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items.length()").value(0));
	}

	@Test
	void clearCartEmptiesIt() throws Exception {
		String token = createUserAndLogin();
		addItem(token, productId, 3);

		mockMvc.perform(delete("/api/v1/cart").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items.length()").value(0))
			.andExpect(jsonPath("$.totalAmount").value(0));
	}

	@Test
	void addingInactiveProductIsRejected() throws Exception {
		String token = createUserAndLogin();
		Product product = productRepository.findById(productId).orElseThrow();
		product.setStatus(ProductStatus.INACTIVE);
		productRepository.save(product);

		mockMvc.perform(post("/api/v1/cart/items")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("productId", productId.toString(), "quantity", 1))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PRODUCT_UNAVAILABLE"));
	}

	@Test
	void addingUnknownProductReturns404() throws Exception {
		String token = createUserAndLogin();

		mockMvc.perform(post("/api/v1/cart/items")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("productId", UUID.randomUUID().toString(), "quantity", 1))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
	}

	@Test
	void cartIsIsolatedPerUser() throws Exception {
		String tokenA = createUserAndLogin();
		String tokenB = createUserAndLogin();

		addItem(tokenA, productId, 4);

		mockMvc.perform(get("/api/v1/cart").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items.length()").value(0));

		mockMvc.perform(get("/api/v1/cart").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items.length()").value(1));
	}

	@Test
	void anonymousAccessIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/cart"))
			.andExpect(status().isUnauthorized());
	}

	private void addItem(String token, UUID productId, int quantity) throws Exception {
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
