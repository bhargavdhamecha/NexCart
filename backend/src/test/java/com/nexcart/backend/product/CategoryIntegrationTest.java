package com.nexcart.backend.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexcart.backend.inventory.repository.InventoryRepository;
import com.nexcart.backend.product.domain.Category;
import com.nexcart.backend.product.repository.CategoryRepository;
import com.nexcart.backend.product.repository.ProductImageRepository;
import com.nexcart.backend.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class CategoryIntegrationTest {

	private MockMvc mockMvc;

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

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
			.apply(SecurityMockMvcConfigurers.springSecurity())
			.build();
		// Products (from any test class — this DB is shared across the whole test run, and no
		// class cleans up after its own *last* test) hold an FK to categories, so they must go
		// first or categoryRepository.deleteAll() below fails with a constraint violation.
		productImageRepository.deleteAll();
		inventoryRepository.deleteAll();
		productRepository.deleteAll();
		categoryRepository.deleteAll();
	}

	@Test
	void listCategoriesIsPublic() throws Exception {
		categoryRepository.save(Category.create("Electronics"));
		categoryRepository.save(Category.create("Books"));

		mockMvc.perform(get("/api/v1/categories"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2));
	}
}
