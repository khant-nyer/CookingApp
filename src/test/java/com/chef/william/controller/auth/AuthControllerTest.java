package com.chef.william.controller.auth;

import com.chef.william.dto.auth.RegisterUserRequest;
import com.chef.william.dto.auth.RegisterUserResponse;
import com.chef.william.exception.DuplicateResourceException;
import com.chef.william.exception.GlobalExceptionHandler;
import com.chef.william.service.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void registerShouldReturnCreatedForValidPayload() throws Exception {
        RegisterUserResponse response = RegisterUserResponse.builder()
                .id(1L)
                .email("chef@example.com")
                .userName("chef")
                .cognitoSub("sub-1")
                .status("PENDING_EMAIL_VERIFICATION")
                .build();

        when(authService.register(any(RegisterUserRequest.class), any(String.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "chef@example.com",
                                  "userName": "chef",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("chef@example.com"))
                .andExpect(jsonPath("$.cognitoSub").value("sub-1"));
    }

    @Test
    void registerShouldReturnValidationErrorShapeForInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "userName": "",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.userName").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void registerShouldReturnConflictWhenDuplicateUser() throws Exception {
        when(authService.register(any(RegisterUserRequest.class), any(String.class)))
                .thenThrow(new DuplicateResourceException("User", "email", "chef@example.com"));

        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("chef@example.com");
        request.setUserName("chef");
        request.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/register")
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void registerShouldReturnBadRequestWhenIdempotencyKeyMissing() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "chef@example.com",
                                  "userName": "chef",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Idempotency-Key header is required for registration requests"));
    }
}
