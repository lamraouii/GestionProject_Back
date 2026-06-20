package com.ensao.gestionprojet.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InvitationResponseDto {

    private String id;
    private Long invitationId;
    private String type;
    private String title;
    private String description;
    private String role;
    private String status;
    private Long invitedById;
    private String invitedByName;
    private LocalDateTime createdAt;
}
