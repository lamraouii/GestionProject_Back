package com.ensao.gestionprojet.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VelocityDto {

    private Long sprintId;

    private String sprintNom;

    private Integer velocite;
}

