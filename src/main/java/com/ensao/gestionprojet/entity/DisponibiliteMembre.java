package com.ensao.gestionprojet.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(
        name = "disponibilite_membre",
        uniqueConstraints = @UniqueConstraint(
                name = "uc_sprint_utilisateur",
                columnNames = {"sprint_id", "utilisateur_id"}
        )
)
public class DisponibiliteMembre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id", nullable = false)
    private Sprint sprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private Integer heuresDisponibles;
}
