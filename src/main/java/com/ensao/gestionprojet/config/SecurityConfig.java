package com.ensao.gestionprojet.config;


import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.boot.autoconfigure.security.servlet.PathRequest;


@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Désactive le CSRF (nécessaire pour les APIs REST testées sur Postman)
                .csrf(csrf -> csrf.disable())

                // 2. Configure les règles d'accès aux URLs
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()

                        // Autorise les routes publiques
                        .requestMatchers("/error").permitAll()
                        // Autorise tout le monde à accéder aux URLs qui commencent par /api/auth/
                        .requestMatchers("/api/auth/**").permitAll()
                        // Toutes les autres URLs de l'application nécessiteront d'être connecté
                        .anyRequest().authenticated()
                );

        return http.build();
    }


}