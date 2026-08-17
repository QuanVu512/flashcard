package com.flashcardapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("test")
class FlashcardApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void publicAuthPagesRender() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("example@gmail.com")));

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("example1@-.*")));
    }

    @Test
    void authenticatedUserCanCreateAndPracticeFlashcardSet() throws Exception {
        String email = "test-" + UUID.randomUUID() + "@flashcard.local";
        register(email);

        mockMvc.perform(get("/sets/new").with(user(email)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nhập từ vựng")));

        MvcResult result = mockMvc.perform(post("/sets")
                        .with(user(email))
                        .with(csrf())
                        .param("title", "Vocabulary Set")
                        .param("description", "Practice set")
                        .param("cards[0].term", "growth")
                        .param("cards[0].definition", "su phat trien")
                        .param("cards[1].term", "coordination")
                        .param("cards[1].definition", "su phoi hop")
                        .param("cards[2].term", "memory")
                        .param("cards[2].definition", "tri nho")
                        .param("cards[3].term", "attention")
                        .param("cards[3].definition", "su chu y"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/sets/*"))
                .andReturn();

        String setUrl = result.getResponse().getRedirectedUrl();
        assertThat(setUrl).isNotBlank();

        mockMvc.perform(get(setUrl).with(user(email)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Flashcards")))
                .andExpect(content().string(containsString("Learn")))
                .andExpect(content().string(containsString("Test")))
                .andExpect(content().string(containsString("Đảo thứ tự")));

        mockMvc.perform(get(setUrl + "/learn").with(user(email)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Chọn đáp án")));

        mockMvc.perform(get(setUrl + "/test/setup").with(user(email)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Số câu hỏi")));

        mockMvc.perform(get(setUrl + "/test?questionCount=3&minutes=5").with(user(email)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-practice-mode=\"test\"")));
    }

    @Test
    void regularUserCannotOpenAdminDashboard() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@flashcard.local";
        register(email);

        mockMvc.perform(get("/admin").with(user(email).roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    @Test
    void adminCanOpenDashboard() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@flashcard.local";
        register(email);

        mockMvc.perform(get("/admin").with(user(email).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bảng quản trị")))
                .andExpect(content().string(containsString("Tài khoản")));
    }

    @Test
    void adminCanOpenUserDetailFromDashboard() throws Exception {
        String email = "detail-admin-" + UUID.randomUUID() + "@flashcard.local";
        register(email);

        MvcResult dashboard = mockMvc.perform(get("/admin").with(user(email).roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        Matcher matcher = Pattern.compile("/admin/users/[0-9a-fA-F-]{36}")
                .matcher(dashboard.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();

        mockMvc.perform(get(matcher.group()).with(user(email).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Thông tin tài khoản")))
                .andExpect(content().string(containsString("Hoạt động học tập")));
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
        String ownerEmail = "owner-" + UUID.randomUUID() + "@flashcard.local";
        String visitorEmail = "visitor-" + UUID.randomUUID() + "@flashcard.local";
        register(ownerEmail);
        register(visitorEmail);

        MvcResult result = mockMvc.perform(post("/sets")
                        .with(user(ownerEmail))
                        .with(csrf())
                        .param("title", "Private Set")
                        .param("cards[0].term", "private")
                        .param("cards[0].definition", "rieng tu"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        mockMvc.perform(get(result.getResponse().getRedirectedUrl()).with(user(visitorEmail)))
                .andExpect(status().isNotFound());
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("displayName", "Test User")
                        .param("email", email)
                        .param("password", "secret123")
                        .param("confirmPassword", "secret123"))
                .andExpect(status().is3xxRedirection());
    }
}
