package com.nexcart.backend.product.service;

import com.nexcart.backend.product.dto.ProductImageSummary;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ProductImageService {

	ProductImageSummary upload(UUID productId, MultipartFile file);

	void delete(UUID productId, UUID imageId);

	List<ProductImageSummary> listByProduct(UUID productId);

	/** Batched variant of listByProduct — one query for a whole page of products, used by
	 *  ProductCatalogCacheImpl to avoid an N+1 on a cache miss. Products with no images are
	 *  simply absent from the returned map. */
	Map<UUID, List<ProductImageSummary>> listByProducts(Collection<UUID> productIds);
}
