package com.ensao.gestionprojet.serviceImp;

import com.ensao.gestionprojet.dto.CreateTacheRequestDto;
import com.ensao.gestionprojet.dto.TacheResponseDto;
import com.ensao.gestionprojet.dto.UpdateStatutTacheRequestDto;
import com.ensao.gestionprojet.dto.KanbanBoardDto;
import com.ensao.gestionprojet.entity.*;
import com.ensao.gestionprojet.enums.*;
import com.ensao.gestionprojet.repository.*;
import com.ensao.gestionprojet.service.TacheService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TacheServiceImpl implements TacheService {

    private final TacheRepository tacheRepository;
    private final ProjetRepository projetRepository;
    private final MembreProjetRepository membreProjetRepository;
    private final SprintRepository sprintRepository;
    private final UtilisateurRepo utilisateurRepository;
    private final AuthHelper authHelper;

    // ============================================================
    //  US10 — Créer une tâche (MANAGER)
    // ============================================================
    @Override
    @Transactional
    public TacheResponseDto creerTache(CreateTacheRequestDto request) {

        Utilisateur manager = authHelper.getUtilisateurCourant();

        Projet projet = projetRepository.findById(request.getProjetId())
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));

        // Vérifier que le projet est ACTIVE
        if (projet.getStatut() != StatutProjet.ACTIVE) {
            throw new RuntimeException("Impossible de créer des tâches — le projet n'est pas actif");
        }

        // Vérifier que l'utilisateur est MANAGER du projet
        verifierManager(manager.getId(), projet.getId());

        Tache tache = new Tache();
        tache.setTitre(request.getTitre());
        tache.setDescription(request.getDescription());
        tache.setProjet(projet);
        tache.setCreateur(manager);
        tache.setStatut(StatutTache.TODO);

        // Priorité (défaut : MEDIUM)
        if (request.getPriorite() != null) {
            tache.setPriorite(PrioriteTache.valueOf(request.getPriorite().toUpperCase()));
        }

        // Story points
        tache.setStoryPoints(request.getStoryPoints());

        // Sprint (optionnel)
        if (request.getSprintId() != null) {
            Sprint sprint = sprintRepository.findById(request.getSprintId())
                    .orElseThrow(() -> new RuntimeException("Sprint introuvable"));

            if (!sprint.getProjet().getId().equals(projet.getId())) {
                throw new RuntimeException("Le sprint n'appartient pas à ce projet");
            }
            tache.setSprint(sprint);
        }

        // Assignation (optionnelle)
        if (request.getUtilisateurAssigneId() != null) {
            Utilisateur assigne = utilisateurRepository.findById(request.getUtilisateurAssigneId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur assigné introuvable"));

            // Vérifier que l'assigné est membre ACCEPTED du projet
            membreProjetRepository
                    .findByUtilisateurIdAndProjetIdAndStatut(
                            assigne.getId(), projet.getId(), StatutInvitation.ACCEPTED)
                    .orElseThrow(() -> new RuntimeException(
                            "L'utilisateur assigné doit être membre accepté du projet"));

            tache.setUtilisateurAssigne(assigne);
        }

        Tache savedTache = tacheRepository.save(tache);

        return toDto(savedTache);
    }

    // ============================================================
    //  US11 — Assigner une tâche à un membre (MANAGER)
    // ============================================================
    @Override
    @Transactional
    public TacheResponseDto assignerTache(Long tacheId, Long utilisateurId) {

        Utilisateur manager = authHelper.getUtilisateurCourant();

        Tache tache = tacheRepository.findById(tacheId)
                .orElseThrow(() -> new RuntimeException("Tâche introuvable"));

        // Vérifier que l'utilisateur courant est MANAGER du projet
        verifierManager(manager.getId(), tache.getProjet().getId());

        // Vérifier que l'utilisateur cible est membre ACCEPTED du projet
        Utilisateur assigne = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        membreProjetRepository
                .findByUtilisateurIdAndProjetIdAndStatut(
                        assigne.getId(), tache.getProjet().getId(), StatutInvitation.ACCEPTED)
                .orElseThrow(() -> new RuntimeException(
                        "L'utilisateur doit être membre accepté du projet pour être assigné"));

        tache.setUtilisateurAssigne(assigne);
        Tache savedTache = tacheRepository.save(tache);

        return toDto(savedTache);
    }

    // ============================================================
    //  US12 — Mettre à jour le statut d'une tâche
    //         (MANAGER ou membre assigné)
    // ============================================================
    @Override
    @Transactional
    public TacheResponseDto mettreAJourStatut(Long tacheId, UpdateStatutTacheRequestDto request) {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();

        Tache tache = tacheRepository.findById(tacheId)
                .orElseThrow(() -> new RuntimeException("Tâche introuvable"));

        // Vérifier : l'utilisateur est MANAGER du projet OU l'assigné de la tâche
        boolean estManager = membreProjetRepository
                .findByUtilisateurIdAndProjetIdAndRole(
                        utilisateur.getId(), tache.getProjet().getId(), RoleProjet.MANAGER)
                .isPresent();

        boolean estAssigne = tache.getUtilisateurAssigne() != null
                && tache.getUtilisateurAssigne().getId().equals(utilisateur.getId());

        if (!estManager && !estAssigne) {
            throw new RuntimeException("Seul le Manager ou le membre assigné peut modifier le statut");
        }

        StatutTache nouveauStatut = StatutTache.valueOf(request.getStatut().toUpperCase());
        StatutTache ancienStatut = tache.getStatut();

        // Appliquer le changement de statut avec mise à jour des timestamps métriques
        tache.setStatut(nouveauStatut);

        // TODO → IN_PROGRESS : enregistrer le début du travail
        if (ancienStatut == StatutTache.TODO && nouveauStatut == StatutTache.IN_PROGRESS) {
            tache.setDateDebutTravail(LocalDateTime.now());
        }

        // IN_PROGRESS → DONE : enregistrer la date de complétion
        if (nouveauStatut == StatutTache.DONE && tache.getDateCompletion() == null) {
            tache.setDateCompletion(LocalDateTime.now());
        }

        // Si on repasse en TODO (cas de réouverture), on réinitialise
        if (nouveauStatut == StatutTache.TODO) {
            tache.setDateDebutTravail(null);
            tache.setDateCompletion(null);
        }

        Tache savedTache = tacheRepository.save(tache);

        return toDto(savedTache);
    }

    // ============================================================
    //  Récupérer toutes les tâches d'un projet
    // ============================================================
    @Override
    public List<TacheResponseDto> getTachesProjet(Long projetId) {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();

        // Vérifier que l'utilisateur est membre du projet
        membreProjetRepository
                .findByUtilisateurIdAndProjetIdAndStatut(
                        utilisateur.getId(), projetId, StatutInvitation.ACCEPTED)
                .orElseThrow(() -> new RuntimeException("Accès refusé — vous n'êtes pas membre de ce projet"));

        return tacheRepository.findByProjetId(projetId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ============================================================
    //  Récupérer les tâches du backlog (sprint_id = NULL)
    // ============================================================
    @Override
    public List<TacheResponseDto> getTachesBacklog(Long projetId) {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();

        membreProjetRepository
                .findByUtilisateurIdAndProjetIdAndStatut(
                        utilisateur.getId(), projetId, StatutInvitation.ACCEPTED)
                .orElseThrow(() -> new RuntimeException("Accès refusé — vous n'êtes pas membre de ce projet"));

        return tacheRepository.findByProjetIdAndSprintIsNull(projetId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ============================================================
    //  US23 — Obtenir la vue Kanban Board
    // ============================================================
    @Override
    public KanbanBoardDto getKanbanBoard(Long projetId) {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();

        // Vérifier que l'utilisateur est membre ACCEPTED du projet
        membreProjetRepository
                .findByUtilisateurIdAndProjetIdAndStatut(
                        utilisateur.getId(), projetId, StatutInvitation.ACCEPTED)
                .orElseThrow(() -> new RuntimeException("Accès refusé — vous n'êtes pas membre de ce projet"));

        List<Tache> tasks = tacheRepository.findByProjetId(projetId);

        List<TacheResponseDto> todo = new java.util.ArrayList<>();
        List<TacheResponseDto> inProgress = new java.util.ArrayList<>();
        List<TacheResponseDto> done = new java.util.ArrayList<>();

        for (Tache t : tasks) {
            TacheResponseDto dto = toDto(t);
            if (t.getStatut() == StatutTache.TODO) {
                todo.add(dto);
            } else if (t.getStatut() == StatutTache.IN_PROGRESS) {
                inProgress.add(dto);
            } else if (t.getStatut() == StatutTache.DONE) {
                done.add(dto);
            }
        }

        return KanbanBoardDto.builder()
                .todo(todo)
                .inProgress(inProgress)
                .done(done)
                .build();
    }

    // ============================================================
    //  Helpers privés
    // ============================================================

    private void verifierManager(Long utilisateurId, Long projetId) {
        membreProjetRepository
                .findByUtilisateurIdAndProjetIdAndRole(utilisateurId, projetId, RoleProjet.MANAGER)
                .orElseThrow(() -> new RuntimeException("Seul le Manager du projet peut effectuer cette action"));
    }

    private TacheResponseDto toDto(Tache tache) {
        String assigneNom = null;
        Long assigneId = null;

        if (tache.getUtilisateurAssigne() != null) {
            assigneId = tache.getUtilisateurAssigne().getId();
            assigneNom = tache.getUtilisateurAssigne().getPrenom()
                    + " " + tache.getUtilisateurAssigne().getNom();
        }

        return TacheResponseDto.builder()
                .id(tache.getId())
                .titre(tache.getTitre())
                .description(tache.getDescription())
                .statut(tache.getStatut().name())
                .priorite(tache.getPriorite().name())
                .projetId(tache.getProjet().getId())
                .sprintId(tache.getSprint() != null ? tache.getSprint().getId() : null)
                .utilisateurAssigneId(assigneId)
                .utilisateurAssigneNom(assigneNom)
                .storyPoints(tache.getStoryPoints())
                .dateCreation(tache.getDateCreation())
                .dateDebutTravail(tache.getDateDebutTravail())
                .dateCompletion(tache.getDateCompletion())
                .build();
    }
}
