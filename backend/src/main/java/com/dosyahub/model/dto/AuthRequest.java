package com.dosyahub.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {
    
    @NotBlank(message = "E-posta adresi zorunludur")
    @Email(message = "Geçerli bir e-posta adresi giriniz")
    private String email;
    
    @NotBlank(message = "Şifre zorunludur")
    @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır")
    private String password;
} 