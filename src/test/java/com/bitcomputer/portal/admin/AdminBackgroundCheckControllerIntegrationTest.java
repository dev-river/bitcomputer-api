package com.bitcomputer.portal.admin;

import com.bitcomputer.portal.bgc.BackgroundCheck;
import com.bitcomputer.portal.bgc.BackgroundCheckMapper;
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

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// Transactional so rerun_whenPendingExists_returns409's inserted PENDING background_check row for
// EMP-010 rolls back after the test, instead of leaking into the shared H2 instance and affecting
// other tests that read EMP-010's background-check history. Same rollback pattern already used
// elsewhere on this branch.
@Transactional
class AdminBackgroundCheckControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EmployeeMapper employeeMapper;
    @Autowired private BackgroundCheckMapper backgroundCheckMapper;

    private String login(String loginId, String password) throws Exception {
        var body = objectMapper.createObjectNode().put("loginId", loginId).put("password", password);
        var result = mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content(body.toString())).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("token").asText();
    }

    @Test
    void rerun_whenPendingExists_returns409() throws Exception {
        String token = login("ADMIN-001", "BitComputer123!");
        int employeeId = employeeMapper.findByEmpNo("EMP-010").getId();

        BackgroundCheck pending = new BackgroundCheck();
        pending.setEmployeeId(employeeId);
        pending.setStatus("PENDING");
        pending.setRequestedAt(LocalDateTime.now());
        pending.setTriggeredByAccountId(1);
        backgroundCheckMapper.insert(pending);

        mockMvc.perform(post("/admin/employees/" + employeeId + "/background-checks")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("ERR_DUPLICATE"));
    }

    @Test
    void history_returnsSeededMaskingDemoRecordForEmp001() throws Exception {
        String token = login("ADMIN-001", "BitComputer123!");
        int employeeId = employeeMapper.findByEmpNo("EMP-001").getId();

        mockMvc.perform(get("/admin/employees/" + employeeId + "/background-checks")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].status").value("CLEAR"));
    }

    @Test
    void asEmployee_isForbidden() throws Exception {
        String token = login("EMP-001", "ChangeMe123!");
        int employeeId = employeeMapper.findByEmpNo("EMP-001").getId();

        mockMvc.perform(get("/admin/employees/" + employeeId + "/background-checks")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }
}
