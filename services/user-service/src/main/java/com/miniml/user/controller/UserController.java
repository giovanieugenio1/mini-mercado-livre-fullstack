package com.miniml.user.controller;

import com.miniml.user.dto.RegisterRequest;
import com.miniml.user.dto.RegisterResponse;
import com.miniml.user.service.UserRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@Tag(name = "Usuários", description = "Cadastro de usuários")
public class UserController {

    private final UserRegistrationService registrationService;

    public UserController(UserRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    @Operation(summary = "Cadastrar novo usuário")
    public ResponseEntity<RegisterResponse> register(@RequestBody @Valid RegisterRequest request) {
        RegisterResponse response = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
