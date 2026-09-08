package com.nexcart.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexcart.backend.user.domain.User;
import com.nexcart.backend.user.domain.UserStatus;
import com.nexcart.backend.user.repository.RefreshSessionRepository;
import com.nexcart.backend.user.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class AuthIntegrationTest {

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
	private WebApplicationContext webApplicationContext;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
			.apply(SecurityMockMvcConfigurers.springSecurity())
			.build();
		refreshSessionRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void registerCreatesUserHashesPasswordAndSetsRefreshCookie() throws Exception {
		MvcResult result = register("Bhargav", "Patel", "BHARGAV@example.com", "Password1");

		JsonNode body = readBody(result);
		User savedUser = userRepository.findByEmail("bhargav@example.com").orElseThrow();

		assertThat(savedUser.getPasswordHash()).isNotEqualTo("Password1");
		assertThat(passwordEncoder.matches("Password1", savedUser.getPasswordHash())).isTrue();
		assertThat(refreshSessionRepository.count()).isEqualTo(1);

		assertThat(body.get("user").get("email").asText()).isEqualTo("bhargav@example.com");
		assertThat(body.get("accessToken").asText()).isNotBlank();
		assertThat(body.get("tokenType").asText()).isEqualTo("Bearer");
		assertThat(body.get("expiresIn").asLong()).isEqualTo(900);

		assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
			.contains("nexcart_refresh_token=")
			.contains("HttpOnly")
			.contains("Secure")
			.contains("SameSite=Strict")
			.contains("Path=/api/v1/auth");
	}

	@Test
	void registerRejectsDuplicateEmail() throws Exception {
		register("Bhargav", "Patel", "bhargav@example.com", "Password1");

		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of(
					"firstName", "New",
					"lastName", "User",
					"email", "BHARGAV@example.com",
					"password", "Password1"
				))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("USER_EMAIL_ALREADY_EXISTS"));
	}

	@Test
	void loginSucceedsForActiveUser() throws Exception {
		seedUser("bhargav@example.com", "Password1", UserStatus.ACTIVE);

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of(
					"email", "bhargav@example.com",
					"password", "Password1"
				))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.user.email").value("bhargav@example.com"))
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("nexcart_refresh_token=")));

		assertThat(refreshSessionRepository.count()).isEqualTo(1);
	}

	@Test
	void loginFailsForWrongPassword() throws Exception {
		seedUser("bhargav@example.com", "Password1", UserStatus.ACTIVE);

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of(
					"email", "bhargav@example.com",
					"password", "WrongPassword1"
				))))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
	}

	@Test
	void loginFailsForDisabledUser() throws Exception {
		seedUser("bhargav@example.com", "Password1", UserStatus.DISABLED);

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of(
					"email", "bhargav@example.com",
					"password", "Password1"
				))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("AUTH_ACCOUNT_DISABLED"));
	}

	@Test
	void refreshRotatesTokenAndRejectsTheOldOne() throws Exception {
		MvcResult registerResult = register("Bhargav", "Patel", "bhargav@example.com", "Password1");
		String oldRefreshToken = extractRefreshToken(registerResult);

		MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new MockCookie("nexcart_refresh_token", oldRefreshToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andReturn();

		String newRefreshToken = extractRefreshToken(refreshResult);
		assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);

		mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new MockCookie("nexcart_refresh_token", oldRefreshToken)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_INVALID_SESSION"));
	}

	@Test
	void logoutRevokesRefreshTokenAndClearsCookie() throws Exception {
		MvcResult registerResult = register("Bhargav", "Patel", "bhargav@example.com", "Password1");
		String refreshToken = extractRefreshToken(registerResult);

		mockMvc.perform(post("/api/v1/auth/logout")
				.cookie(new MockCookie("nexcart_refresh_token", refreshToken)))
			.andExpect(status().isNoContent())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));

		mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new MockCookie("nexcart_refresh_token", refreshToken)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_INVALID_SESSION"));
	}

	@Test
	void meReturnsAuthenticatedUser() throws Exception {
		MvcResult registerResult = register("Bhargav", "Patel", "bhargav@example.com", "Password1");
		String accessToken = readBody(registerResult).get("accessToken").asText();

		mockMvc.perform(get("/api/v1/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("bhargav@example.com"))
			.andExpect(jsonPath("$.role").value("CUSTOMER"));
	}

	@Test
	void protectedEndpointRejectsMissingAccessToken() throws Exception {
		mockMvc.perform(get("/api/v1/users/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
	}

	@Test
	void multipleDeviceSessionsRemainIndependent() throws Exception {
		MvcResult registerResult = register("Bhargav", "Patel", "bhargav@example.com", "Password1");
		String firstSessionRefreshToken = extractRefreshToken(registerResult);

		MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of(
					"email", "bhargav@example.com",
					"password", "Password1"
				))))
			.andExpect(status().isOk())
			.andReturn();
		String secondSessionRefreshToken = extractRefreshToken(loginResult);

		mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new MockCookie("nexcart_refresh_token", firstSessionRefreshToken)))
			.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new MockCookie("nexcart_refresh_token", secondSessionRefreshToken)))
			.andExpect(status().isOk());
	}

	private MvcResult register(String firstName, String lastName, String email, String password) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(Map.of(
					"firstName", firstName,
					"lastName", lastName,
					"email", email,
					"password", password
				))))
			.andExpect(status().isCreated())
			.andReturn();
	}

	private User seedUser(String email, String password, UserStatus status) {
		User user = User.create("Bhargav", "Patel", email, passwordEncoder.encode(password));
		user.setStatus(status);
		return userRepository.save(user);
	}

	private JsonNode readBody(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private String extractRefreshToken(MvcResult result) {
		String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
		assertThat(setCookie).isNotBlank();
		String firstPart = setCookie.split(";", 2)[0];
		return firstPart.substring(firstPart.indexOf('=') + 1);
	}

	private String json(Object value) throws Exception {
		return objectMapper.writeValueAsString(value);
	}
}
