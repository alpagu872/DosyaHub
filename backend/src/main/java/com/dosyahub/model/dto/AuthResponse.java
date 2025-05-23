package com.dosyahub.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String token;
    private String tokenType;
    private long expiresIn;
} 