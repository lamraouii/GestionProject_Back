package com.ensao.gestionprojet.entity;

import com.ensao.gestionprojet.enums.StatutProjet;
import com.ensao.gestionprojet.enums.TypeProjet;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Projet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;          // NULL si projet personnel

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createur_id", nullable = false)
    private Utilisateur createur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeProjet type;                // PERSONAL, COMPANY

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutProjet statut;            // PENDING, ACTIVE, REJECTED, ARCHIVED

    private LocalDate dateDebut;

    private LocalDate dateFin;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    // Relations
    @OneToMany(mappedBy = "projet", fetch = FetchType.LAZY)
    private List<MembreProjet> membres;

    @OneToMany(mappedBy = "projet", fetch = FetchType.LAZY)
    private List<Sprint> sprints;

    @OneToMany(mappedBy = "projet", fetch = FetchType.LAZY)
    private List<Tache> taches;
}