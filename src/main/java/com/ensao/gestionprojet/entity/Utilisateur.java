package com.ensao.gestionprojet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String motDePasse;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(length = 500)
    private String urlAvatar;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(nullable = false)
    @Builder.Default
    private Boolean estActif = false;

    // Relations
    @OneToMany(mappedBy = "createur", fetch = FetchType.LAZY)
    private List<Entreprise> entreprisesCreees;

    @OneToMany(mappedBy = "utilisateur", fetch = FetchType.LAZY)
    private List<MembreEntreprise> appartenancesEntreprises;

    @OneToMany(mappedBy = "utilisateur", fetch = FetchType.LAZY)
    private List<MembreProjet> appartenancesProjets;
}