package com.flashcardapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashcardapp.entity.OtpPurpose;
import com.flashcardapp.service.OtpMailService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("test")
class FlashcardApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OtpMailService otpMailService;

    @Test
    void contextLoads() {
    }

    @Test
    void publicSpaRoutesServeStaticIndex() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/library"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/views/auth/login.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đăng nhập")));
    }

    @Test
    void authenticatedUserCanCreateAndPracticeFlashcardSetThroughRestApi() throws Exception {
        Cookie authCookie = registerAndCookie("test-" + UUID.randomUUID() + "@flashcard.local");

        mockMvc.perform(get("/api/library").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").exists());

        MvcResult result = mockMvc.perform(post("/api/sets")
                        .cookie(authCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Vocabulary Set",
                                  "description": "Practice set",
                                  "cards": [
                                    {"term": "growth", "definition": "su phat trien"},
                                    {"term": "coordination", "definition": "su phoi hop"},
                                    {"term": "memory", "definition": "tri nho"},
                                    {"term": "attention", "definition": "su chu y"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.cards.length()").value(4))
                .andReturn();

        String setId = read(result, "id");

        mockMvc.perform(get("/api/sets/" + setId + "/study").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Vocabulary Set"));

        mockMvc.perform(get("/api/sets/" + setId + "/learn").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(4));

        mockMvc.perform(get("/api/sets/" + setId + "/test/setup").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxQuestions").value(4));

        mockMvc.perform(get("/api/sets/" + setId + "/test?questionCount=3&minutes=5")
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionCount").value(3));
    }

    @Test
    void regularUserCannotOpenAdminApi() throws Exception {
        Cookie authCookie = registerAndCookie("user-" + UUID.randomUUID() + "@flashcard.local");

        mockMvc.perform(get("/api/admin").cookie(authCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanOpenDashboardApi() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@flashcard.local";

        mockMvc.perform(get("/api/admin").with(user(email).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.userCount").exists())
                .andExpect(jsonPath("$.users").isArray());
    }

    @Test
    void apiWithoutLoginReturnsJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/api/translation/suggest")
                        .param("text", "hello")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("Bạn cần đăng nhập")));
    }

    @Test
    void userCannotOpenAnotherUsersFlashcardSet() throws Exception {
        Cookie ownerCookie = registerAndCookie("owner-" + UUID.randomUUID() + "@flashcard.local");
        Cookie visitorCookie = registerAndCookie("visitor-" + UUID.randomUUID() + "@flashcard.local");

        MvcResult result = mockMvc.perform(post("/api/sets")
                        .cookie(ownerCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Private Set",
                                  "cards": [{"term": "private", "definition": "rieng tu"}]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String setId = read(result, "id");

        mockMvc.perform(get("/api/sets/" + setId).cookie(visitorCookie))
                .andExpect(status().isNotFound());
    }

    private Cookie registerAndCookie(String email) throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Test User",
                                  "email": "%s",
                                  "password": "secret123",
                                  "confirmPassword": "secret123"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OTP_REQUIRED"))
                .andExpect(jsonPath("$.challengeId").exists())
                .andReturn();

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(otpMailService, timeout(5000))
                .send(eq(email), otpCaptor.capture(), eq(OtpPurpose.EMAIL_VERIFICATION), anyLong());
        clearInvocations(otpMailService);

        String challengeId = read(registration, "challengeId");
        MvcResult result = mockMvc.perform(post("/api/auth/otp/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "challengeId": "%s",
                                  "code": "%s",
                                  "rememberDevice": false
                                }
                                """.formatted(challengeId, otpCaptor.getValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.session.user.email").value(email))
                .andReturn();

        MockCookie cookie = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
                .map(MockCookie::parse)
                .filter(candidate -> "flashcard_access_token".equals(candidate.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Response không có auth cookie HttpOnly"));
        assertThat(cookie.isHttpOnly()).isTrue();
        return cookie;
    }

    private String read(MvcResult result, String field) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        String value = json.path(field).asText();
        assertThat(value).isNotBlank();
        return value;
    }

}
