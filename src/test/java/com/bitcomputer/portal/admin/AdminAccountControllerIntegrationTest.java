package com.bitcomputer.portal.admin;

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

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// Transactional so createAccount_forEmployeeWithoutOne_returnsTempPasswordThatLogsIn's new EMP-005
// account rolls back after the test, instead of leaking into the shared H2 instance for later
// runs of this same test (or other tests) that also assume EMP-005 has no account yet.
@Transactional
class AdminAccountControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EmployeeMapper employeeMapper;

    private String login(String loginId, String password) throws Exception {
        var body = objectMapper.createObjectNode().put("loginId", loginId).put("password", password);
        var result = mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content(body.toString())).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("token").asText();
    }

    @Test
    void createAccount_forEmployeeWithoutOne_returnsTempPasswordThatLogsIn() throws Exception {
        String adminToken = login("ADMIN-001", "ChangeMe123!");
        int emp005Id = employeeMapper.findByEmpNo("EMP-005").getId();

        var body = objectMapper.createObjectNode().put("employeeId", emp005Id);
        var result = mockMvc.perform(post("/admin/accounts").header("Authorization", "Bearer " + adminToken)
                .contentType(APPLICATION_JSON).content(body.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.loginId").value("EMP-005"))
            .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        String tempPassword = data.get("tempPassword").asText();

        var loginBody = objectMapper.createObjectNode().put("loginId", "EMP-005").put("password", tempPassword);
        mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content(loginBody.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.mustChangePassword").value(true));
    }

    @Test
    void createAccount_forEmployeeThatAlreadyHasOne_returns409() throws Exception {
        String adminToken = login("ADMIN-001", "ChangeMe123!");
        int emp001Id = employeeMapper.findByEmpNo("EMP-001").getId();

        var body = objectMapper.createObjectNode().put("employeeId", emp001Id);
        mockMvc.perform(post("/admin/accounts").header("Authorization", "Bearer " + adminToken)
                .contentType(APPLICATION_JSON).content(body.toString()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("ERR_DUPLICATE"));
    }
}
