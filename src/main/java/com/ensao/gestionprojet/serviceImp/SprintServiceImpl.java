package com.ensao.gestionprojet.serviceImp;

import com.ensao.gestionprojet.dto.*;
import com.ensao.gestionprojet.entity.*;
import com.ensao.gestionprojet.enums.*;
import com.ensao.gestionprojet.helpers.AuthHelper;
import com.ensao.gestionprojet.mapper.SprintMapper;
import com.ensao.gestionprojet.repository.*;
import com.ensao.gestionprojet.service.SprintService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SprintServiceImpl implements SprintService {

    private final SprintRepository sprintRepository;
    private final ProjetRepository projetRepository;
    private final MembreProjetRepository membreProjetRepository;
    private final TacheRepository tacheRepository;
    private final DisponibiliteMembreRepository disponibiliteMembreRepository;
    private final UtilisateurRepo utilisateurRepository;
    private final AuthHelper authHelper;
    private final SprintMapper sprintMapper;

    // ============================================================
    //  US13 — Créer un sprint (MANAGER)
    // ============================================================
    @Override
    @Transactional
    public SprintResponseDto creerSprint(CreateSprintRequestDto request) {

        Utilisateur createur = authHelper.getUtilisateurCourant();

        Projet projet = projetRepository.findById(request.getProjetId())
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));

        // Vérifier que le projet est ACTIVE
        if (projet.getStatut() != StatutProjet.ACTIVE) {
            throw new RuntimeException("Impossible de créer un sprint — le projet n'est pas actif");
        }

        // Vérifier que l'utilisateur courant est MANAGER du projet
        verifierManager(createur.getId(), projet.getId());

        // Contrainte métier : dateFin > dateDebut
        if (!request.getDateFin().isAfter(request.getDateDebut())) {
            throw new RuntimeException("La date de fin doit être strictement supérieure à la date de début");
        }

        Sprint sprint = Sprint.builder()
                .projet(projet)
                .nom(request.getNom())
                .objectif(request.getObjectif())
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .statut(StatutSprint.PLANNED)
                .createur(createur)
                .build();

        Sprint savedSprint = sprintRepository.save(sprint);

        return sprintMapper.toDto(savedSprint);
    }

    // ============================================================
    //  US14 — Ajouter des tâches au sprint (MANAGER)
    // ============================================================
    @Override
    @Transactional
    public SprintResponseDto ajouterTaches(Long sprintId, AddTachesSprintRequestDto request) {

        Utilisateur manager = authHelper.getUtilisateurCourant();

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint introuvable"));

        Projet projet = sprint.getProjet();

        // Vérifier que l'utilisateur courant est MANAGER du projet
        verifierManager(manager.getId(), projet.getId());

        // Associer chaque tâche
        for (Long tacheId : request.getTacheIds()) {
            Tache tache = tacheRepository.findById(tacheId)
                    .orElseThrow(() -> new RuntimeException("Tâche introuvable avec l'ID : " + tacheId));

            // Vérifier que la tâche appartient au même projet
            if (!tache.getProjet().getId().equals(projet.getId())) {
                throw new RuntimeException("La tâche " + tache.getTitre() + " n'appartient pas à ce projet");
            }

            // Vérifier que la tâche est au statut TODO
            if (tache.getStatut() != StatutTache.TODO) {
                throw new RuntimeException("Seules les tâches au statut TODO peuvent être ajoutées à un sprint");
            }

            tache.setSprint(sprint);
            tacheRepository.save(tache);
        }

        return sprintMapper.toDto(sprint);
    }

    // ============================================================
    //  US15 — Définir la disponibilité des membres (MANAGER)
    // ============================================================
    @Override
    @Transactional
    public void definirDisponibilite(Long sprintId, List<DisponibiliteMembreRequestDto> request) {

        Utilisateur manager = authHelper.getUtilisateurCourant();

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint introuvable"));

        Projet projet = sprint.getProjet();

        // Vérifier que l'utilisateur courant est MANAGER du projet
        verifierManager(manager.getId(), projet.getId());

        for (DisponibiliteMembreRequestDto item : request) {
            Utilisateur targetUser = utilisateurRepository.findById(item.getUtilisateurId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec l'ID : " + item.getUtilisateurId()));

            // Vérifier que l'utilisateur cible est membre ACCEPTED du projet
            membreProjetRepository.findByUtilisateurIdAndProjetIdAndStatut(
                    targetUser.getId(), projet.getId(), StatutInvitation.ACCEPTED)
                    .orElseThrow(() -> new RuntimeException("L'utilisateur " + targetUser.getPrenom() + " " + targetUser.getNom() + " n'est pas membre actif de ce projet"));

            // Trouver ou créer la disponibilité
            DisponibiliteMembre disponibilite = disponibiliteMembreRepository
                    .findBySprintIdAndUtilisateurId(sprint.getId(), targetUser.getId())
                    .orElseGet(() -> DisponibiliteMembre.builder()
                            .sprint(sprint)
                            .utilisateur(targetUser)
                            .build());

            disponibilite.setHeuresDisponibles(item.getHeuresDisponibles());
            disponibiliteMembreRepository.save(disponibilite);
        }
    }

    // ============================================================
    //  Récupérer tous les sprints d'un projet
    // ============================================================
    @Override
    public List<SprintResponseDto> getSprintsProjet(Long projetId) {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();

        // Vérifier que l'utilisateur est membre ACCEPTED du projet
        membreProjetRepository.findByUtilisateurIdAndProjetIdAndStatut(
                utilisateur.getId(), projetId, StatutInvitation.ACCEPTED)
                .orElseThrow(() -> new RuntimeException("Accès refusé — vous n'êtes pas membre de ce projet"));

        return sprintRepository.findByProjetId(projetId)
                .stream()
                .map(sprintMapper::toDto)
                .collect(Collectors.toList());
    }



    @Override
    @Transactional
    public void activerSprint(Long sprintId) {

        // 1. Current user
        Utilisateur utilisateur = authHelper.getUtilisateurCourant();
        // 2. Sprint
        Sprint sprint = sprintRepository
                .findById(sprintId)
                .orElseThrow(() ->
                        new RuntimeException("Sprint introuvable"));

        Long projetId = sprint.getProjet().getId();

        // 3. Verify MANAGER role
        membreProjetRepository
                .findByUtilisateurIdAndProjetIdAndRole(
                        utilisateur.getId(),
                        projetId,
                        RoleProjet.MANAGER
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Seul un MANAGER peut activer un sprint"
                        ));

        // 4. Sprint must be PLANNED
        if (sprint.getStatut() != StatutSprint.PLANNED) {

            throw new RuntimeException(
                    "Seuls les sprints PLANNED peuvent être activés"
            );
        }

        // 5. Only one ACTIVE sprint per project
        boolean activeSprintExists =
                sprintRepository.existsByProjetIdAndStatut(
                        projetId,
                        StatutSprint.ACTIVE
                );

        if (activeSprintExists) {

            throw new RuntimeException(
                    "Un sprint actif existe déjà pour ce projet"
            );
        }

        // 6. Activate sprint
        sprint.setStatut(
                StatutSprint.ACTIVE
        );

        sprintRepository.save(
                sprint
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SprintResponseDto getSprintActif(Long projetId) {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();

        membreProjetRepository
                .findByUtilisateurIdAndProjetId(
                        utilisateur.getId(),
                        projetId
                )
                .orElseThrow(() ->
                        new RuntimeException("Accès refusé"));

        Sprint sprint = sprintRepository
                .findByProjetIdAndStatut(
                        projetId,
                        StatutSprint.ACTIVE
                )
                .orElseThrow(() ->
                        new RuntimeException("Aucun sprint actif"));

        List<TacheResponseDto> taches = sprint.getTaches()
                .stream()
                .map(t -> {
                    TacheResponseDto dto = new TacheResponseDto();
                    dto.setId(t.getId());
                    dto.setTitre(t.getTitre());
                    dto.setStatut(t.getStatut());

                    if (t.getUtilisateurAssigne() != null) {
                        dto.setUtilisateurAssigneNom(t.getUtilisateurAssigne().getNom());
                    }

                    return dto;
                })
                .toList();

        return sprintMapper.toDtoWithTasks(sprint, taches);
    }
    // ============================================================
    //  Helpers privés
    // ============================================================

    private void verifierManager(Long utilisateurId, Long projetId) {
        membreProjetRepository.findByUtilisateurIdAndProjetIdAndRole(utilisateurId, projetId, RoleProjet.MANAGER)
                .orElseThrow(() -> new RuntimeException("Seul le Manager du projet peut effectuer cette action"));
    }


}
