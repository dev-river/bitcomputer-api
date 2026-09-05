package com.bitcomputer.portal.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_withSeededAdminCredentials_returnsToken() throws Exception {
        var body = objectMapper.writeValueAsString(new Object() {
            public String loginId = "ADMIN-001";
            public String password = "ChangeMe123!";
        });

        mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").isNotEmpty())
            .andExpect(jsonPath("$.data.mustChangePassword").value(false));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        var body = objectMapper.writeValueAsString(new Object() {
            public String loginId = "ADMIN-001";
            public String password = "wrong";
        });

        mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("ERR_UNAUTHORIZED"));
    }
}
