package com.ensao.gestionprojet.entity;

import com.ensao.gestionprojet.enums.RoleEntreprise;
import com.ensao.gestionprojet.enums.StatutInvitation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"utilisateur_id", "entreprise_id"}))
public class MembreEntreprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleEntreprise role;            // ADMIN, MEMBER

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutInvitation statut;        // PENDING, ACCEPTED, REJECTED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invite_par_id")
    private Utilisateur invitePar;

    @Column(nullable = false)
    private LocalDateTime dateInvitation;

    private LocalDateTime dateReponse;
}