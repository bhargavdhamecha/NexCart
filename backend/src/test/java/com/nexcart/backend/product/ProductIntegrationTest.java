package com.nexcart.backend.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.backend.inventory.repository.InventoryRepository;
import com.nexcart.backend.product.domain.Category;
import com.nexcart.backend.product.domain.ProductStatus;
import com.nexcart.backend.product.repository.CategoryRepository;
import com.nexcart.backend.product.repository.ProductImageRepository;
import com.nexcart.backend.product.repository.ProductRepository;
import com.nexcart.backend.user.domain.User;
import com.nexcart.backend.user.domain.UserRole;
import com.nexcart.backend.user.repository.RefreshSessionRepository;
import com.nexcart.backend.user.repository.UserRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class ProductIntegrationTest {

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
	private WebApplicationContext webApplicationContext;

	private UUID categoryId;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
			.apply(SecurityMockMvcConfigurers.springSecurity())
			.build();
		productImageRepository.deleteAll();
		inventoryRepository.deleteAll();
		productRepository.deleteAll();
		categoryRepository.deleteAll();
		refreshSessionRepository.deleteAll();
		userRepository.deleteAll();
		categoryId = categoryRepository.save(Category.create("Electronics")).getId();
	}

	@Test
	void createProductAsAdminSucceeds() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		JsonNode body = createProduct(adminToken, "Test Widget", categoryId, 10);

		assertThat(body.get("title").asText()).isEqualTo("Test Widget");
		assertThat(body.get("availableQuantity").asInt()).isEqualTo(10);
		assertThat(body.get("category").get("id").asText()).isEqualTo(categoryId.toString());
		assertThat(body.get("images").isEmpty()).isTrue();

		UUID productId = UUID.fromString(body.get("productId").asText());
		assertThat(inventoryRepository.findByProductId(productId)).isPresent();
		assertThat(inventoryRepository.findByProductId(productId).orElseThrow().getAvailableQuantity()).isEqualTo(10);
	}

	@Test
	void createProductAsNonAdminIsForbidden() throws Exception {
		String customerToken = createUserAndLogin(UserRole.CUSTOMER);
		mockMvc.perform(post("/api/v1/products")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(productPayload("X", categoryId, 1))))
			.andExpect(status().isForbidden());
	}

	@Test
	void createProductAnonymousIsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(productPayload("X", categoryId, 1))))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void createProductValidatesRequiredFields() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		mockMvc.perform(post("/api/v1/products")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(productPayload("", categoryId, 1))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void createProductRejectsUnknownCategoryId() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		mockMvc.perform(post("/api/v1/products")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(productPayload("X", UUID.randomUUID(), 1))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
	}

	@Test
	void listProductsIsPublicAndReturnsOnlyActiveProducts() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		JsonNode active = createProduct(adminToken, "Active Product", categoryId, 5);
		JsonNode toDeactivate = createProduct(adminToken, "Inactive Product", categoryId, 5);
		String inactiveId = toDeactivate.get("productId").asText();
		mockMvc.perform(delete("/api/v1/products/" + inactiveId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
			.andExpect(status().isNoContent());

		MvcResult result = mockMvc.perform(get("/api/v1/products")).andExpect(status().isOk()).andReturn();
		List<String> ids = productIds(result);
		assertThat(ids).contains(active.get("productId").asText());
		assertThat(ids).doesNotContain(inactiveId);
	}

	@Test
	void getProductByIdReturns404ForUnknownId() throws Exception {
		mockMvc.perform(get("/api/v1/products/" + UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
	}

	@Test
	void getProductByIdReturnsProductEvenIfInactive() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		JsonNode created = createProduct(adminToken, "To Deactivate", categoryId, 3);
		String id = created.get("productId").asText();
		mockMvc.perform(delete("/api/v1/products/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/products/" + id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("INACTIVE"));
	}

	@Test
	void listRespectsCategoryFilter() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		UUID otherCategoryId = categoryRepository.save(Category.create("Books")).getId();
		JsonNode inElectronics = createProduct(adminToken, "Laptop", categoryId, 5);
		createProduct(adminToken, "Novel", otherCategoryId, 5);

		MvcResult result = mockMvc.perform(get("/api/v1/products").param("categoryId", categoryId.toString()))
			.andExpect(status().isOk())
			.andReturn();
		assertThat(productIds(result)).containsExactly(inElectronics.get("productId").asText());
	}

	@Test
	void listRespectsPagination() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		for (int i = 0; i < 5; i++) {
			createProduct(adminToken, "Product " + i, categoryId, 1);
		}
		mockMvc.perform(get("/api/v1/products").param("page", "0").param("size", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.totalElements").value(5))
			.andExpect(jsonPath("$.totalPages").value(3))
			.andExpect(jsonPath("$.number").value(0));
	}

	@Test
	void updateProductAsAdminSucceeds() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		JsonNode created = createProduct(adminToken, "Old Title", categoryId, 5);
		String id = created.get("productId").asText();

		mockMvc.perform(put("/api/v1/products/" + id)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of(
					"title", "New Title",
					"description", "Updated description",
					"price", 29.99,
					"categoryId", categoryId.toString(),
					"status", "ACTIVE"
				))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("New Title"));
	}

	@Test
	void updateProductAsNonAdminIsForbidden() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		String customerToken = createUserAndLogin(UserRole.CUSTOMER);
		JsonNode created = createProduct(adminToken, "Title", categoryId, 5);
		String id = created.get("productId").asText();

		mockMvc.perform(put("/api/v1/products/" + id)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of(
					"title", "New",
					"description", "D",
					"price", 1.0,
					"categoryId", categoryId.toString(),
					"status", "ACTIVE"
				))))
			.andExpect(status().isForbidden());
	}

	@Test
	void deleteProductAsAdminSoftDeletes() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		JsonNode created = createProduct(adminToken, "To Delete", categoryId, 5);
		UUID id = UUID.fromString(created.get("productId").asText());

		mockMvc.perform(delete("/api/v1/products/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
			.andExpect(status().isNoContent());

		assertThat(productRepository.findById(id)).isPresent();
		assertThat(productRepository.findById(id).orElseThrow().getStatus()).isEqualTo(ProductStatus.INACTIVE);

		MvcResult listResult = mockMvc.perform(get("/api/v1/products")).andReturn();
		assertThat(productIds(listResult)).doesNotContain(id.toString());

		mockMvc.perform(get("/api/v1/products/" + id)).andExpect(status().isOk());
	}

	@Test
	void deleteProductAsNonAdminIsForbidden() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		String customerToken = createUserAndLogin(UserRole.CUSTOMER);
		JsonNode created = createProduct(adminToken, "Title", categoryId, 5);
		String id = created.get("productId").asText();

		mockMvc.perform(delete("/api/v1/products/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
			.andExpect(status().isForbidden());
	}

	@Test
	void adjustInventoryAsAdminSucceeds() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		JsonNode created = createProduct(adminToken, "Title", categoryId, 5);
		String id = created.get("productId").asText();

		mockMvc.perform(patch("/api/v1/products/" + id + "/inventory")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("availableQuantity", 99))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.availableQuantity").value(99));

		mockMvc.perform(get("/api/v1/products/" + id))
			.andExpect(jsonPath("$.availableQuantity").value(99));
	}

	@Test
	void productDetailStockStaysFreshDespiteCatalogCaching() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		JsonNode created = createProduct(adminToken, "Cached Product", categoryId, 5);
		String id = created.get("productId").asText();

		// First GET populates the (cached) catalog entry for this product.
		mockMvc.perform(get("/api/v1/products/" + id))
			.andExpect(jsonPath("$.availableQuantity").value(5));

		// Stock changes via a completely different path — not a product create/update/delete,
		// so it never evicts the catalog cache.
		mockMvc.perform(patch("/api/v1/products/" + id + "/inventory")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("availableQuantity", 42))))
			.andExpect(status().isOk());

		// Second GET must show the new stock immediately, proving it was never part of what got
		// cached — the whole point of keeping ProductCatalogEntry stock-free.
		mockMvc.perform(get("/api/v1/products/" + id))
			.andExpect(jsonPath("$.availableQuantity").value(42));
	}

	@Test
	void productListReflectsEditImmediatelyAfterCacheEviction() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		JsonNode created = createProduct(adminToken, "Original Title", categoryId, 5);
		String id = created.get("productId").asText();

		// Populate the list cache with the original title.
		MvcResult before = mockMvc.perform(get("/api/v1/products")).andExpect(status().isOk()).andReturn();
		assertThat(before.getResponse().getContentAsString()).contains("Original Title");

		mockMvc.perform(put("/api/v1/products/" + id)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of(
					"title", "Edited Title",
					"description", "Updated",
					"price", 29.99,
					"categoryId", categoryId.toString(),
					"status", "ACTIVE"
				))))
			.andExpect(status().isOk());

		// Same list query as before — must reflect the edit immediately, proving update()'s
		// @CacheEvict actually busts the previously-cached page rather than serving it stale.
		MvcResult after = mockMvc.perform(get("/api/v1/products")).andExpect(status().isOk()).andReturn();
		assertThat(after.getResponse().getContentAsString()).contains("Edited Title");
		assertThat(after.getResponse().getContentAsString()).doesNotContain("Original Title");
	}

	@Test
	void adjustInventoryAsNonAdminIsForbidden() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		String customerToken = createUserAndLogin(UserRole.CUSTOMER);
		JsonNode created = createProduct(adminToken, "Title", categoryId, 5);
		String id = created.get("productId").asText();

		mockMvc.perform(patch("/api/v1/products/" + id + "/inventory")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("availableQuantity", 99))))
			.andExpect(status().isForbidden());
	}

	@Test
	void uploadImageAsAdminSucceeds() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		JsonNode created = createProduct(adminToken, "Title", categoryId, 5);
		String id = created.get("productId").asText();

		mockMvc.perform(multipart("/api/v1/products/{id}/images", id)
				.file(imageFile("photo.jpg"))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.url").isNotEmpty())
			.andExpect(jsonPath("$.primary").value(true));

		assertThat(productImageRepository.count()).isEqualTo(1);
	}

	@Test
	void uploadImageAsNonAdminIsForbidden() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		String customerToken = createUserAndLogin(UserRole.CUSTOMER);
		JsonNode created = createProduct(adminToken, "Title", categoryId, 5);
		String id = created.get("productId").asText();

		mockMvc.perform(multipart("/api/v1/products/{id}/images", id)
				.file(imageFile("photo.jpg"))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
			.andExpect(status().isForbidden());
	}

	@Test
	void uploadImageWithInvalidContentTypeIsRejected() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		JsonNode created = createProduct(adminToken, "Title", categoryId, 5);
		String id = created.get("productId").asText();
		MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "bytes".getBytes());

		mockMvc.perform(multipart("/api/v1/products/{id}/images", id)
				.file(file)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_IMAGE_FILE"));
	}

	@Test
	void uploadImagePastCapIsRejected() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		JsonNode created = createProduct(adminToken, "Title", categoryId, 5);
		String id = created.get("productId").asText();

		for (int i = 0; i < 6; i++) {
			mockMvc.perform(multipart("/api/v1/products/{id}/images", id)
					.file(imageFile("photo" + i + ".jpg"))
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
				.andExpect(status().isCreated());
		}

		mockMvc.perform(multipart("/api/v1/products/{id}/images", id)
				.file(imageFile("photo6.jpg"))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("TOO_MANY_IMAGES"));
	}

	@Test
	void deleteImageAsAdminSucceeds() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		JsonNode created = createProduct(adminToken, "Title", categoryId, 5);
		String id = created.get("productId").asText();
		String imageId = uploadImage(adminToken, id);

		mockMvc.perform(delete("/api/v1/products/" + id + "/images/" + imageId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
			.andExpect(status().isNoContent());

		assertThat(productImageRepository.count()).isEqualTo(0);
	}

	@Test
	void deleteImageAsNonAdminIsForbidden() throws Exception {
		String adminToken = createUserAndLogin(UserRole.ADMIN);
		String customerToken = createUserAndLogin(UserRole.CUSTOMER);
		JsonNode created = createProduct(adminToken, "Title", categoryId, 5);
		String id = created.get("productId").asText();
		String imageId = uploadImage(adminToken, id);

		mockMvc.perform(delete("/api/v1/products/" + id + "/images/" + imageId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
			.andExpect(status().isForbidden());
	}

	private String createUserAndLogin(UserRole role) throws Exception {
		String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
		User user = User.create("Test", "User", email, passwordEncoder.encode("Password1"));
		user.setRole(role);
		userRepository.save(user);

		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of("email", email, "password", "Password1"))))
			.andExpect(status().isOk())
			.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
	}

	private JsonNode createProduct(String adminToken, String title, UUID categoryId, int initialQuantity)
		throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/products")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(productPayload(title, categoryId, initialQuantity))))
			.andExpect(status().isCreated())
			.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private String uploadImage(String adminToken, String productId) throws Exception {
		MvcResult result = mockMvc.perform(multipart("/api/v1/products/{id}/images", productId)
				.file(imageFile("photo.jpg"))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
			.andExpect(status().isCreated())
			.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
	}

	// A real (tiny) JPEG, not fake bytes — ImageCompressor decodes every JPEG/PNG upload via
	// ImageIO now, so a non-image payload would correctly be rejected as invalid.
	private MockMultipartFile imageFile(String filename) throws Exception {
		BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "jpg", out);
		return new MockMultipartFile("file", filename, "image/jpeg", out.toByteArray());
	}

	private Map<String, Object> productPayload(String title, UUID categoryId, int initialQuantity) {
		return Map.of(
			"title", title,
			"description", "A great product",
			"price", 19.99,
			"categoryId", categoryId.toString(),
			"initialQuantity", initialQuantity
		);
	}

	private List<String> productIds(MvcResult result) throws Exception {
		JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
		List<String> ids = new ArrayList<>();
		content.forEach(node -> ids.add(node.get("productId").asText()));
		return ids;
	}

	private String json(Object value) throws Exception {
		return objectMapper.writeValueAsString(value);
	}
}
