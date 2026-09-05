package com.bitcomputer.portal.employee;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// Transactional so patchMe_updatesContactInfoAndWritesChangeLog's and
// patchMe_withPartialBody_leavesOtherFieldsUnchanged's mutations of EMP-001's contact info (and
// the change-log rows they write) roll back after each test, instead of leaking into the shared
// H2 instance and affecting other tests that read EMP-001's phone/email/address or change log.
// Same rollback pattern already used elsewhere on this branch.
@Transactional
class EmployeeSelfControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EmployeeChangeLogMapper changeLogMapper;
    @Autowired private EmployeeMapper employeeMapper;

    private String login(String loginId, String password) throws Exception {
        var body = objectMapper.createObjectNode().put("loginId", loginId).put("password", password);
        var result = mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content(body.toString())).andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("data").get("token").asText();
    }

    @Test
    void getMe_returnsOwnInfo_notReadableFieldsAreExcludedFromUpdate() throws Exception {
        String token = login("EMP-001", "ChangeMe123!");

        mockMvc.perform(get("/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.empNo").value("EMP-001"))
            .andExpect(jsonPath("$.data.name").value("김민준"));
    }

    @Test
    void patchMe_updatesContactInfoAndWritesChangeLog() throws Exception {
        String token = login("EMP-001", "ChangeMe123!");
        int employeeId = employeeMapper.findByEmpNo("EMP-001").getId();

        var body = objectMapper.createObjectNode()
            .put("phone", "010-9999-8888")
            .put("email", "minjun.kim@bitcomputer.kr")
            .put("address", "서울시 마포구");

        mockMvc.perform(patch("/me").header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON).content(body.toString()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/me").header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$.data.phone").value("010-9999-8888"));

        var logs = changeLogMapper.findByEmployeeId(employeeId);
        assertThat(logs).extracting("fieldName").contains("phone", "email", "address");
    }

    @Test
    void adminRole_cannotAccessMeEndpoint() throws Exception {
        String token = login("ADMIN-001", "ChangeMe123!");
        mockMvc.perform(get("/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void patchMe_withPartialBody_leavesOtherFieldsUnchanged() throws Exception {
        String token = login("EMP-001", "ChangeMe123!");

        var fullBody = objectMapper.createObjectNode()
            .put("phone", "010-1111-2222")
            .put("email", "before@bitcomputer.kr")
            .put("address", "서울시 종로구");
        mockMvc.perform(patch("/me").header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON).content(fullBody.toString()))
            .andExpect(status().isOk());

        var partialBody = objectMapper.createObjectNode().put("phone", "010-3333-4444");
        mockMvc.perform(patch("/me").header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON).content(partialBody.toString()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/me").header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$.data.phone").value("010-3333-4444"))
            .andExpect(jsonPath("$.data.email").value("before@bitcomputer.kr"))
            .andExpect(jsonPath("$.data.address").value("서울시 종로구"));
    }

    @Test
    void patchMe_withAddressOverMaxLength_returns400Validation() throws Exception {
        String token = login("EMP-001", "ChangeMe123!");

        String tooLongAddress = "가".repeat(201);
        var body = objectMapper.createObjectNode().put("address", tooLongAddress);

        mockMvc.perform(patch("/me").header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON).content(body.toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("ERR_VALIDATION"));
    }
}
