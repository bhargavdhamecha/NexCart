package com.nexcart.backend.security.jwt;

import com.nexcart.backend.common.exception.AuthException;
import com.nexcart.backend.user.domain.User;
import com.nexcart.backend.user.domain.UserStatus;
import com.nexcart.backend.user.repository.UserRepository;
import com.nexcart.backend.user.service.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenService jwtTokenService;
	private final UserRepository userRepository;
	private final AuthenticationEntryPoint authenticationEntryPoint;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		try {
			String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
			if (authorization != null && authorization.startsWith("Bearer ")
				&& SecurityContextHolder.getContext().getAuthentication() == null) {
				String accessToken = authorization.substring(7);
				UUID userId = jwtTokenService.parseUserId(accessToken);
				User user = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
					.orElseThrow(AuthException::invalidAccessToken);

				AuthenticatedUser authenticatedUser = new AuthenticatedUser(
					user.getId(),
					user.getEmail(),
					user.getFirstName(),
					user.getLastName(),
					user.getRole()
				);

				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					authenticatedUser,
					null,
					List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
				);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}

			filterChain.doFilter(request, response);
		}
		catch (AuthException exception) {
			request.setAttribute("auth_error_code", exception.getErrorCode().name());
			request.setAttribute("auth_error_message", exception.getMessage());
			authenticationEntryPoint.commence(request, response,
				new BadCredentialsException(exception.getMessage(), exception));
		}
	}
}
