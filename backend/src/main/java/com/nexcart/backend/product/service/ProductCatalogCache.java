package com.nexcart.backend.product.service;

import com.nexcart.backend.common.dto.PageResponse;
import com.nexcart.backend.product.dto.ProductCatalogEntry;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/**
 * The cached half of product browsing — catalog data only (title/description/price/category/
 * images/status), never live stock (see ProductCatalogEntry's javadoc for why). Deliberately a
 * separate bean from ProductServiceImpl/ProductServiceImpl, not just a private method there:
 * Spring's @Cacheable only intercepts calls that go through the Spring proxy, so a self-invoked
 * call (one method on a class calling another @Cacheable method on that SAME class) silently
 * skips caching entirely — this interface exists so ProductServiceImpl always calls it through
 * its injected proxy.
 */
public interface ProductCatalogCache {

	PageResponse<ProductCatalogEntry> getPage(UUID categoryId, String search, Pageable pageable);

	ProductCatalogEntry getEntry(UUID productId);
}
