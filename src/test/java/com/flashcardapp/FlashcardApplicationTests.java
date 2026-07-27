package com.flashcardapp;

import com.flashcardapp.dto.RegisterRequest;
import com.flashcardapp.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class FlashcardApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

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

    private void register(String email) {
        RegisterRequest request = new RegisterRequest();
        request.setDisplayName("Test User");
        request.setEmail(email);
        request.setPassword("secret123");
        request.setConfirmPassword("secret123");
        userService.register(request);
    }
}
