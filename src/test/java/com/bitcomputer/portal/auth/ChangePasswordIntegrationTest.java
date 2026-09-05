package com.bitcomputer.portal.auth;

import com.bitcomputer.portal.employee.EmployeeMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChangePasswordIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EmployeeMapper employeeMapper;

    private String loginAndGetToken(String loginId, String password) throws Exception {
        var loginBody = objectMapper.createObjectNode().put("loginId", loginId).put("password", password);
        var result = mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content(loginBody.toString()))
            .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("data").get("token").asText();
    }

    @Test
    void changePassword_thenReloginWithNewPassword_succeeds() throws Exception {
        String token = loginAndGetToken("EMP-001", "ChangeMe123!");

        var changeBody = objectMapper.createObjectNode()
            .put("currentPassword", "ChangeMe123!")
            .put("newPassword", "NewPass456!");

        mockMvc.perform(post("/auth/change-password")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(changeBody.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        String newToken = loginAndGetToken("EMP-001", "NewPass456!");
        org.assertj.core.api.Assertions.assertThat(newToken).isNotBlank();
    }

    @Test
    void terminatedEmployee_isRejectedOnNextRequestEvenWithValidToken() throws Exception {
        String token = loginAndGetToken("EMP-001", "ChangeMe123!");

        var employee = employeeMapper.findByEmpNo("EMP-001");
        employeeMapper.updateStatus(employee.getId(), "TERMINATED", LocalDateTime.now());

        var changeBody = objectMapper.createObjectNode()
            .put("currentPassword", "ChangeMe123!")
            .put("newPassword", "NewPass456!");

        mockMvc.perform(post("/auth/change-password")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(changeBody.toString()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("ERR_FORBIDDEN"));
    }

    @Test
    void invalidToken_isRejectedWith401() throws Exception {
        mockMvc.perform(post("/auth/change-password")
                .header("Authorization", "Bearer not-a-real-token")
                .contentType(APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_withNoAuthorizationHeaderAtAll_returnsAppJsonEnvelope() throws Exception {
        mockMvc.perform(post("/auth/change-password").contentType(APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("ERR_UNAUTHORIZED"));
    }
}
