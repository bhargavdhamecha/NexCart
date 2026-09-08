package com.nexcart.backend.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.backend.common.dto.ApiError;
import com.nexcart.backend.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException authException) throws IOException {
		String code = (String) request.getAttribute("auth_error_code");
		String message = (String) request.getAttribute("auth_error_message");

		ApiError apiError = ApiError.of(
			HttpStatus.UNAUTHORIZED,
			code != null ? code : ErrorCode.AUTH_UNAUTHORIZED.name(),
			message != null ? message : "Authentication is required to access this resource",
			request
		);

		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), apiError);
	}
}
