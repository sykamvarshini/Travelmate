package com.travelmate.controller;

import com.travelmate.config.JwtAuthenticationFilter;
import com.travelmate.config.JwtTokenProvider;
import com.travelmate.config.SecurityConfig;
import com.travelmate.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HealthController.class, properties = "app.jwt.secret=health-test-secret-key-at-least-32-characters")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class HealthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getHealthIsPublicAndReturnsOnlyStatus() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}"));
    }

    @Test
    void otherMethodsOnHealthRemainProtected() throws Exception {
        mockMvc.perform(post("/health"))
                .andExpect(status().is4xxClientError());
    }
}
