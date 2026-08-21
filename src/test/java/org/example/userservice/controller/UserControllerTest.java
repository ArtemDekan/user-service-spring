package org.example.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.userservice.dto.UserRequest;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.exception.EmailAlreadyExistsException;
import org.example.userservice.exception.UserNotFoundException;
import org.example.userservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserResponse sampleResponse() {
        return new UserResponse(1L, "Alex", "alex@mail.com", 30, LocalDateTime.now());
    }

    @Test
    @DisplayName("POST /api/users создаёт пользователя и возвращает 201")
    void createUser_returns201() throws Exception {
        when(userService.createUser(any(UserRequest.class))).thenReturn(sampleResponse());

        UserRequest request = new UserRequest("Alex", "alex@mail.com", 30);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alex"))
                .andExpect(jsonPath("$.email").value("alex@mail.com"));
    }

    @Test
    @DisplayName("POST /api/users возвращает 400 при некорректном теле запроса")
    void createUser_returns400_whenInvalidBody() throws Exception {
        UserRequest invalid = new UserRequest("", "not-an-email", 0);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.age").exists());

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("POST /api/users возвращает 409, если email уже занят")
    void createUser_returns409_whenEmailTaken() throws Exception {
        when(userService.createUser(any(UserRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("alex@mail.com"));

        UserRequest request = new UserRequest("Alex", "alex@mail.com", 30);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET /api/users/{id} возвращает пользователя")
    void getUserById_returnsUser() throws Exception {
        when(userService.getUserById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alex"));
    }

    @Test
    @DisplayName("GET /api/users/{id} возвращает 404, если пользователь не найден")
    void getUserById_returns404_whenNotFound() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/api/users/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/users возвращает список пользователей")
    void getAllUsers_returnsList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("alex@mail.com"));
    }

    @Test
    @DisplayName("PUT /api/users/{id} обновляет пользователя")
    void updateUser_returnsUpdatedUser() throws Exception {
        UserResponse updated = new UserResponse(1L, "Alex Updated", "alex@mail.com", 35, LocalDateTime.now());
        when(userService.updateUser(eq(1L), any(UserRequest.class))).thenReturn(updated);

        UserRequest request = new UserRequest("Alex Updated", "alex@mail.com", 35);

        mockMvc.perform(put("/api/users/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alex Updated"))
                .andExpect(jsonPath("$.age").value(35));
    }

    @Test
    @DisplayName("PUT /api/users/{id} возвращает 404, если пользователь не найден")
    void updateUser_returns404_whenNotFound() throws Exception {
        when(userService.updateUser(eq(99L), any(UserRequest.class)))
                .thenThrow(new UserNotFoundException(99L));

        UserRequest request = new UserRequest("Alex", "alex@mail.com", 30);

        mockMvc.perform(put("/api/users/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/users/{id} удаляет пользователя и возвращает 204")
    void deleteUser_returns204() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    @DisplayName("DELETE /api/users/{id} возвращает 404, если пользователь не найден")
    void deleteUser_returns404_whenNotFound() throws Exception {
        doThrow(new UserNotFoundException(99L)).when(userService).deleteUser(99L);

        mockMvc.perform(delete("/api/users/{id}", 99L))
                .andExpect(status().isNotFound());
    }
}
