package com.ensao.gestionprojet.serviceImp;

import com.ensao.gestionprojet.dto.CreateTacheRequestDto;
import com.ensao.gestionprojet.dto.KanbanBoardDto;
import com.ensao.gestionprojet.dto.TacheResponseDto;
import com.ensao.gestionprojet.dto.UpdateStatutTacheRequestDto;
import com.ensao.gestionprojet.entity.BurndownSnapshot;
import com.ensao.gestionprojet.entity.Projet;
import com.ensao.gestionprojet.entity.Sprint;
import com.ensao.gestionprojet.entity.Tache;
import com.ensao.gestionprojet.entity.Utilisateur;
import com.ensao.gestionprojet.enums.PrioriteTache;
import com.ensao.gestionprojet.enums.StatutInvitation;
import com.ensao.gestionprojet.enums.StatutProjet;
import com.ensao.gestionprojet.enums.StatutSprint;
import com.ensao.gestionprojet.enums.StatutTache;
import com.ensao.gestionprojet.helpers.AuthHelper;
import com.ensao.gestionprojet.helpers.ProjectAccessHelper;
import com.ensao.gestionprojet.repository.BurndownSnapshotRepository;
import com.ensao.gestionprojet.repository.MembreProjetRepository;
import com.ensao.gestionprojet.repository.ProjetRepository;
import com.ensao.gestionprojet.repository.SprintRepository;
import com.ensao.gestionprojet.repository.TacheRepository;
import com.ensao.gestionprojet.repository.UtilisateurRepo;
import com.ensao.gestionprojet.service.TacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final BurndownSnapshotRepository burndownSnapshotRepository;
    private final AuthHelper authHelper;
    private final ProjectAccessHelper projectAccessHelper;

    @Override
    @Transactional
    public TacheResponseDto creerTache(CreateTacheRequestDto request) {

        Utilisateur manager = authHelper.getUtilisateurCourant();

        Projet projet = projetRepository.findById(request.getProjetId())
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));

        if (projet.getStatut() != StatutProjet.ACTIVE) {
            throw new RuntimeException("Impossible de creer des taches - le projet n'est pas actif");
        }

        projectAccessHelper.requireManager(manager, projet);

        Tache tache = new Tache();
        tache.setTitre(request.getTitre());
        tache.setDescription(request.getDescription());
        tache.setProjet(projet);
        tache.setCreateur(manager);
        tache.setStatut(StatutTache.TODO);

        if (request.getPriorite() != null) {
            tache.setPriorite(PrioriteTache.valueOf(request.getPriorite().toUpperCase()));
        }

        tache.setStoryPoints(request.getStoryPoints());

        if (request.getSprintId() != null) {
            Sprint sprint = sprintRepository.findById(request.getSprintId())
                    .orElseThrow(() -> new RuntimeException("Sprint introuvable"));

            if (!sprint.getProjet().getId().equals(projet.getId())) {
                throw new RuntimeException("Le sprint n'appartient pas a ce projet");
            }
            if (sprint.getStatut() != StatutSprint.PLANNED) {
                throw new RuntimeException("Impossible d'ajouter une tache - le sprint est deja active ou cloture");
            }

            tache.setSprint(sprint);
        }

        if (request.getUtilisateurAssigneId() != null) {
            Utilisateur assigne = utilisateurRepository.findById(request.getUtilisateurAssigneId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur assigne introuvable"));

            if (projectAccessHelper.resolveRole(assigne, projet).isEmpty()) {
                throw new RuntimeException("L'utilisateur assigne doit etre membre accepte du projet");
            }

            tache.setUtilisateurAssigne(assigne);
        }

        Tache savedTache = tacheRepository.save(tache);

        if (savedTache.getSprint() != null
                && savedTache.getSprint().getStatut() == StatutSprint.ACTIVE) {
            enregistrerSnapshot(savedTache.getSprint());
        }

        return toDto(savedTache);
    }

    @Override
    @Transactional
    public TacheResponseDto assignerTache(Long tacheId, Long utilisateurId) {

        Utilisateur manager = authHelper.getUtilisateurCourant();

        Tache tache = tacheRepository.findById(tacheId)
                .orElseThrow(() -> new RuntimeException("Tache introuvable"));

        projectAccessHelper.requireManager(manager, tache.getProjet());

        Utilisateur assigne = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (projectAccessHelper.resolveRole(assigne, tache.getProjet()).isEmpty()) {
            throw new RuntimeException("L'utilisateur doit être membre accepté du projet pour être assigné");
        }

        tache.setUtilisateurAssigne(assigne);
        Tache savedTache = tacheRepository.save(tache);

        return toDto(savedTache);
    }

    @Override
    @Transactional
    public TacheResponseDto mettreAJourStatut(Long tacheId, UpdateStatutTacheRequestDto request) {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();

        Tache tache = tacheRepository.findById(tacheId)
                .orElseThrow(() -> new RuntimeException("Tache introuvable"));

        if (tache.getSprint() != null && tache.getSprint().getStatut() != StatutSprint.ACTIVE) {
            throw new RuntimeException("Impossible de modifier le statut - le sprint n'est pas actif");
        }

        projectAccessHelper.requireAccess(utilisateur, tache.getProjet());

        boolean estAssigne = tache.getUtilisateurAssigne() != null
                && tache.getUtilisateurAssigne().getId().equals(utilisateur.getId());

        if (!estAssigne) {
            throw new RuntimeException("Seul le membre assigne a cette tache peut modifier son statut");
        }

        StatutTache nouveauStatut = StatutTache.valueOf(request.getStatut().toUpperCase());

        tache.setStatut(nouveauStatut);

        if (nouveauStatut == StatutTache.IN_PROGRESS && tache.getDateDebutTravail() == null) {
            tache.setDateDebutTravail(LocalDateTime.now());
        }

        if (nouveauStatut == StatutTache.TODO) {
            tache.setDateDebutTravail(null);
            tache.setDateCompletion(null);
        }

        if (nouveauStatut == StatutTache.IN_PROGRESS) {
            tache.setDateCompletion(null);
        }

        if (nouveauStatut == StatutTache.DONE) {
            if (tache.getDateDebutTravail() == null) {
                tache.setDateDebutTravail(LocalDateTime.now());
            }
            if (tache.getDateCompletion() == null) {
                tache.setDateCompletion(LocalDateTime.now());
            }
        }

        Tache savedTache = tacheRepository.save(tache);

        if (savedTache.getSprint() != null
                && savedTache.getSprint().getStatut() == StatutSprint.ACTIVE) {
            enregistrerSnapshot(savedTache.getSprint());
        }

        return toDto(savedTache);
    }

    @Override
    public List<TacheResponseDto> getTachesProjet(Long projetId) {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();
        Projet projet = getProjetOrThrow(projetId);

        projectAccessHelper.requireAccess(utilisateur, projet);

        return tacheRepository.findByProjetId(projetId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TacheResponseDto> getTachesBacklog(Long projetId) {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();
        Projet projet = getProjetOrThrow(projetId);

        projectAccessHelper.requireAccess(utilisateur, projet);

        return tacheRepository.findByProjetIdAndSprintIsNull(projetId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public KanbanBoardDto getKanbanBoard(Long projetId) {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();
        Projet projet = getProjetOrThrow(projetId);

        projectAccessHelper.requireAccess(utilisateur, projet);

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

    private Projet getProjetOrThrow(Long projetId) {
        return projetRepository.findById(projetId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));
    }

    private void enregistrerSnapshot(Sprint sprint) {

        int remainingPoints = sprint.getTaches()
                .stream()
                .filter(t -> t.getStatut() != StatutTache.DONE)
                .mapToInt(t -> t.getStoryPoints() == null ? 0 : t.getStoryPoints())
                .sum();

        LocalDate today = LocalDate.now();

        BurndownSnapshot snapshot = burndownSnapshotRepository
                .findBySprintIdAndDate(sprint.getId(), today)
                .orElseGet(() -> BurndownSnapshot.builder()
                        .sprint(sprint)
                        .date(today)
                        .build());

        snapshot.setRemainingPoints(remainingPoints);

        burndownSnapshotRepository.save(snapshot);
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
                .statut(tache.getStatut())
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
