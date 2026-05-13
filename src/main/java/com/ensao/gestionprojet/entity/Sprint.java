package com.ensao.gestionprojet.entity;

import com.ensao.gestionprojet.enums.StatutSprint;
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
public class Sprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;

    @Column(nullable = false, length = 200)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String objectif;

    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate dateFin;              // Contrainte métier : dateFin > dateDebut (vérifiée en service)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutSprint statut;            // PLANNED, ACTIVE, COMPLETED

    /**
     * Vélocité calculée = somme des storyPoints des tâches DONE dans ce sprint.
     * Un seul sprint ACTIVE autorisé par projet (contrainte vérifiée en service).
     */
    private Integer velocite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createur_id", nullable = false)
    private Utilisateur createur;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    // Relations
    @OneToMany(mappedBy = "sprint", fetch = FetchType.LAZY)
    private List<Tache> taches;
}