package com.ensao.gestionprojet.entity;

import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDateTime;


@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmationToken {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    public Long id;

    @Column(nullable = false, unique = true)
    public String token;

    @Column(name = "expiration_date", nullable = false)
    public LocalDateTime expirationDate;

    @Builder.Default
    @Column(nullable = false)
    private Boolean utilise = false;

    @OneToOne
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;
}
