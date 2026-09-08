package com.nexcart.backend.config;

import com.nexcart.backend.inventory.service.InventoryService;
import com.nexcart.backend.product.domain.Category;
import com.nexcart.backend.product.domain.Product;
import com.nexcart.backend.product.repository.CategoryRepository;
import com.nexcart.backend.product.repository.ProductRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds a handful of categories and products so the frontend has real data to hit once its mock
 * ProductService is removed. Uses the same Category.create/Product.create/InventoryService calls
 * as any other code path (not raw SQL), so BaseEntity's auditing/UUID generation stays consistent.
 * "!test" prevents seed rows from breaking ProductIntegrationTest's exact-count assertions.
 * Seed products ship with zero images — see the plan's Frontend changes for the emoji fallback.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;
	private final InventoryService inventoryService;

	@Override
	public void run(String... args) {
		if (categoryRepository.count() > 0) {
			return;
		}

		Category electronics = categoryRepository.save(Category.create("Electronics"));
		Category fashion = categoryRepository.save(Category.create("Fashion"));
		Category home = categoryRepository.save(Category.create("Home"));
		Category books = categoryRepository.save(Category.create("Books"));

		seed(electronics, "NexBook Air 14", "A slim, fast laptop for everyday work and study.",
			"74999", 12);
		seed(electronics, "Pulse Wireless Earbuds", "Noise-isolating earbuds with 30hr battery life.",
			"2999", 40);
		seed(electronics, "Aperture 4K Action Camera", "Compact 4K camera built for travel and sports.",
			"12499", 18);
		seed(fashion, "Everyday Canvas Sneakers", "Lightweight, breathable sneakers for daily wear.",
			"1899", 60);
		seed(fashion, "Classic Denim Jacket", "A timeless denim jacket that pairs with everything.",
			"2499", 35);
		seed(home, "Aroma Ceramic Diffuser", "Whisper-quiet essential oil diffuser with LED glow.",
			"1299", 50);
		seed(home, "Warmlight Table Lamp", "A soft-glow lamp with a solid oak base.",
			"1799", 25);
		seed(books, "The Quiet Algorithm", "A novel about a programmer who finds unexpected meaning.",
			"499", 100);
	}

	private void seed(Category category, String title, String description, String price, int quantity) {
		Product product = productRepository.save(Product.create(title, description, new BigDecimal(price), category));
		inventoryService.initialize(product.getId(), quantity);
	}
}
