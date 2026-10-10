package com.example.rag.controller;

import com.example.rag.dto.response.BatchGrantResult;
import com.example.rag.dto.response.BatchRevokeResult;
import com.example.rag.security.SecurityBeansConfig;
import com.example.rag.security.SecurityConfig;
import com.example.rag.service.KnowledgeBaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KnowledgeBaseController.class)
@Import({SecurityConfig.class, SecurityBeansConfig.class})
class KnowledgeBaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeBaseService knowledgeBaseService;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanBatchGrant() throws Exception {
        when(knowledgeBaseService.batchGrant(anyLong(), any()))
                .thenReturn(new BatchGrantResult(2, 0, List.of()));

        mockMvc.perform(post("/api/v1/knowledge-bases/1/permissions/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userIds":[2,3],"permission":"READ"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.grantedCount").value(2));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanBatchRevoke() throws Exception {
        when(knowledgeBaseService.batchRevoke(anyLong(), any()))
                .thenReturn(new BatchRevokeResult(2));

        mockMvc.perform(delete("/api/v1/knowledge-bases/1/permissions/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userIds":[2,3]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.revokedCount").value(2));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanSingleGrant() throws Exception {
        mockMvc.perform(post("/api/v1/knowledge-bases/1/grants/5")
                        .param("permission", "READ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void nonAdminCannotBatchGrant() throws Exception {
        mockMvc.perform(post("/api/v1/knowledge-bases/1/permissions/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userIds":[2],"permission":"READ"}
                                """))
                .andExpect(status().isForbidden());
    }
}
