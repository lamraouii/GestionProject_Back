package com.ensao.gestionprojet.entity;

import com.ensao.gestionprojet.enums.RoleProjet;
import com.ensao.gestionprojet.enums.StatutInvitation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"utilisateur_id", "projet_id"}))
public class MembreProjet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleProjet role;                // MANAGER, MEMBER

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutInvitation statut;        // PENDING, ACCEPTED, REJECTED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invite_par_id")
    private Utilisateur invitePar;

    private LocalDateTime dateAdhesion;
}