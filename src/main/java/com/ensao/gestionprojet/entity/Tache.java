package com.ensao.gestionprojet.entity;

import com.ensao.gestionprojet.enums.PrioriteTache;
import com.ensao.gestionprojet.enums.StatutTache;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
@Table(
        name = "task",
        check = @CheckConstraint(
                name = "chk_story_points_positive",
                constraint = "story_points >= 0"
        )
)
@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Tache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;                  // NULL = tâche dans le backlog

    /**
     * Contrainte métier : doit être un MembreProjet avec statut ACCEPTED.
     * Vérification effectuée dans la couche service avant toute assignation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_assigne_id")
    private Utilisateur utilisateurAssigne;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatutTache statut = StatutTache.TODO;      // TODO, IN_PROGRESS, DONE

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PrioriteTache priorite = PrioriteTache.MEDIUM; // LOW, MEDIUM, HIGH, CRITICAL
    private Integer storyPoints;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createur_id", nullable = false)
    private Utilisateur createur;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime dateMiseAJour;
}