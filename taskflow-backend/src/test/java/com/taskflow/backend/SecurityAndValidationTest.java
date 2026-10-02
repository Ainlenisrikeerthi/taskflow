package com.taskflow.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.backend.dto.TaskRequest;
import com.taskflow.backend.model.Role;
import com.taskflow.backend.model.User;
import com.taskflow.backend.repository.UserRepository;
import com.taskflow.backend.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityAndValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUpUsers() {
        userRepository.deleteAll();

        User user = new User();
        user.setName("Regular User");
        user.setEmail("regular@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(Role.USER);
        userRepository.save(user);
        userToken = jwtUtils.generateToken("regular@example.com");

        User admin = new User();
        admin.setName("Admin User");
        admin.setEmail("admin@example.com");
        admin.setPasswordHash(passwordEncoder.encode("password123"));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        adminToken = jwtUtils.generateToken("admin@example.com");
    }

    @Test
    void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/users/me/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUserCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUserCannotCreateTask() throws Exception {
        TaskRequest request = new TaskRequest("Forbidden Task", "Desc", "Inst", LocalDate.now().plusDays(2), null,
                "PUBLISHED");
        mockMvc.perform(post("/api/tasks")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminCanAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testAdminCanPublishTask() throws Exception {
        mockMvc.perform(patch("/api/admin/tasks/999/publish")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUserCannotPublishTask() throws Exception {
        mockMvc.perform(patch("/api/admin/tasks/999/publish")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
