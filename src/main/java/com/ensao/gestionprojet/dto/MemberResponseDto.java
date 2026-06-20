package com.ensao.gestionprojet.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemberResponseDto {

    private Long id;
    private Long userId;
    private String nom;
    private String prenom;
    private String email;
    private String role;
    private String status;
    private Long invitedById;
    private String invitedByName;
    private LocalDateTime invitedAt;
    private LocalDateTime joinedAt;
}
