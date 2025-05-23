package com.dosyahub.service;

import com.dosyahub.dto.AuthRequest;
import com.dosyahub.dto.AuthResponse;
import com.dosyahub.dto.RegisterRequest;
import com.dosyahub.dto.UserDto;
import com.dosyahub.model.User;
import com.dosyahub.repository.UserRepository;
import com.dosyahub.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthResponse login(AuthRequest request) {
        log.info("Kullanıcı giriş denemesi: {}", request.getEmail());
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwt = tokenProvider.generateToken(userDetails);
        
        // Son giriş zamanını güncelle
        User user = userRepository.findByEmail(request.getEmail()).get();
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("Kullanıcı başarıyla giriş yaptı: {}", request.getEmail());
        
        return AuthResponse.builder()
                .token(jwt)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    @Transactional
    public UserDto register(RegisterRequest request) {
        log.info("Yeni kullanıcı kaydı: {}", request.getEmail());
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Bu e-posta adresi zaten kullanılıyor");
        }
        
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("Kullanıcı başarıyla kaydedildi: {}", savedUser.getEmail());
        
        return UserDto.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .build();
    }
} 