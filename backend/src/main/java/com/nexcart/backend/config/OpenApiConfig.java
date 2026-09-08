package com.nexcart.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI setup. UI available at /swagger-ui.html, raw spec at /v3/api-docs.
 */
@Configuration
public class OpenApiConfig {

	private static final String BEARER_SCHEME = "bearerAuth";

	@Bean
	OpenAPI nexcartOpenApi() {
		return new OpenAPI()
			.info(new Info()
				.title("Nexcart API")
				.description("REST API for the Nexcart e-commerce platform")
				.version("v1"))
			.addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
			.components(new Components()
				.addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
					.name(BEARER_SCHEME)
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")));
	}
}
