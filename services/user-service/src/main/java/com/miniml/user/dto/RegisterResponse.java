package com.miniml.user.dto;

public record RegisterResponse(
        String id,
        String firstName,
        String lastName,
        String email,
        String message
) {}
