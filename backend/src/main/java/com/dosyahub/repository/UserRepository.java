package com.dosyahub.repository;

import com.dosyahub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    /**
     * E-posta ile kullanıcı arama
     * @param email Kullanıcı e-posta adresi
     * @return Kullanıcı (varsa)
     */
    Optional<User> findByEmail(String email);
    
    /**
     * E-posta ile kullanıcı varlığını kontrol etme
     * @param email Kullanıcı e-posta adresi
     * @return Kullanıcı varsa true, yoksa false
     */
    boolean existsByEmail(String email);
} 