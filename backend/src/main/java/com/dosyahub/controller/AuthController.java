package com.dosyahub.controller;

import com.dosyahub.dto.AuthRequest;
import com.dosyahub.dto.AuthResponse;
import com.dosyahub.dto.RegisterRequest;
import com.dosyahub.dto.UserDto;
import com.dosyahub.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Kimlik Doğrulama", description = "Kullanıcı kimlik doğrulama işlemleri")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
            summary = "Kullanıcı Girişi",
            description = "Kullanıcı adı ve şifre ile giriş yapılır",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Başarılı giriş"),
                    @ApiResponse(responseCode = "401", description = "Kimlik doğrulama başarısız")
            }
    )
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest loginRequest) {
        AuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/register")
    @Operation(
            summary = "Kullanıcı Kaydı",
            description = "Yeni kullanıcı kaydı oluşturulur",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Kayıt başarılı"),
                    @ApiResponse(responseCode = "400", description = "Geçersiz istek")
            }
    )
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest registerRequest) {
        UserDto userDto = authService.register(registerRequest);
        return ResponseEntity.ok(userDto);
    }
} 