package com.leadflow.backend.controller.user;

import com.leadflow.backend.config.TestSecurityConfig;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.exception.GlobalExceptionHandler;
import com.leadflow.backend.service.user.UserService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    /* ==========================
       USER ROLE
       ========================== */

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403ForUserRole() throws Exception {

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    /* ==========================
       ADMIN ROLE
       ========================== */

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200ForAdminRole() throws Exception {

        Page<User> emptyPage = new PageImpl<>(Collections.emptyList());

        when(userService.listActiveUsers(any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }

    /* ==========================
       NOT AUTHENTICATED
       ========================== */

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }
}