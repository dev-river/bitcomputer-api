package com.bitcomputer.portal.auth;

import com.bitcomputer.portal.employee.EmployeeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// Transactional so login_asTerminatedEmployee_returns403's status change on EMP-001 rolls back
// after the test, instead of leaking into the shared H2 instance and breaking other tests (e.g.
// ChangePasswordIntegrationTest, EmployeeSelfControllerIntegrationTest) that log in as EMP-001 and
// expect it to still be ACTIVE. Same rollback pattern already used elsewhere on this branch.
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Test
    void login_withSeededAdminCredentials_returnsToken() throws Exception {
        var body = objectMapper.writeValueAsString(new Object() {
            public String loginId = "ADMIN-001";
            public String password = "BitComputer123!";
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

    @Test
    void login_asTerminatedEmployee_returns403() throws Exception {
        var employee = employeeMapper.findByEmpNo("EMP-001");
        employeeMapper.updateStatus(employee.getId(), "TERMINATED", LocalDateTime.now());

        var body = objectMapper.writeValueAsString(new Object() {
            public String loginId = "EMP-001";
            public String password = "ChangeMe123!";
        });

        mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("ERR_FORBIDDEN"));
    }
}
