package com.bitcomputer.portal.admin;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// Transactional so terminate_setsStatusAndBlocksSubsequentLogin's permanent-looking status change
// on EMP-001 rolls back after the test — without this it leaked into the shared H2 instance for
// the rest of the suite and broke ChangePasswordIntegrationTest and EmployeeSelfControllerIntegrationTest,
// both of which log in as EMP-001 and expect it to still be ACTIVE. Same rollback pattern already
// used by ChangePasswordIntegrationTest. Found while running the full suite (not just this
// package) after implementing Task 8's brief as given.
@Transactional
class AdminEmployeeControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String login(String loginId, String password) throws Exception {
        var body = objectMapper.createObjectNode().put("loginId", loginId).put("password", password);
        var result = mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content(body.toString())).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("token").asText();
    }

    @Test
    void listEmployees_asAdmin_returnsAllTenSeeded() throws Exception {
        String token = login("ADMIN-001", "ChangeMe123!");
        mockMvc.perform(get("/admin/employees").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(10));
    }

    @Test
    void listEmployees_asEmployee_isForbidden() throws Exception {
        String token = login("EMP-001", "ChangeMe123!");
        mockMvc.perform(get("/admin/employees").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void searchByName_filtersDuplicateNames() throws Exception {
        String token = login("ADMIN-001", "ChangeMe123!");
        mockMvc.perform(get("/admin/employees?name=김민준").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void getDetail_includesEmptyChangeLogForUntouchedEmployee() throws Exception {
        String token = login("ADMIN-001", "ChangeMe123!");
        var listResult = mockMvc.perform(get("/admin/employees?empNo=EMP-003").header("Authorization", "Bearer " + token)).andReturn();
        JsonNode list = objectMapper.readTree(listResult.getResponse().getContentAsString()).get("data");
        int id = list.get(0).get("id").asInt();

        mockMvc.perform(get("/admin/employees/" + id).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.empNo").value("EMP-003"))
            .andExpect(jsonPath("$.data.changeLogs.length()").value(0));
    }

    @Test
    void terminate_setsStatusAndBlocksSubsequentLogin() throws Exception {
        String adminToken = login("ADMIN-001", "ChangeMe123!");
        var listResult = mockMvc.perform(get("/admin/employees?empNo=EMP-001").header("Authorization", "Bearer " + adminToken)).andReturn();
        int id = objectMapper.readTree(listResult.getResponse().getContentAsString()).get("data").get(0).get("id").asInt();

        String employeeToken = login("EMP-001", "ChangeMe123!");

        mockMvc.perform(post("/admin/employees/" + id + "/terminate").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/me").header("Authorization", "Bearer " + employeeToken))
            .andExpect(status().isForbidden());
    }
}
