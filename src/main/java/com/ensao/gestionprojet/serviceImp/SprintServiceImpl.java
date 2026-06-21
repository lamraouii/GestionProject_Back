package com.ensao.gestionprojet.serviceImp;

import com.ensao.gestionprojet.dto.AddTachesSprintRequestDto;
import com.ensao.gestionprojet.dto.BurndownDto;
import com.ensao.gestionprojet.dto.CreateSprintRequestDto;
import com.ensao.gestionprojet.dto.DisponibiliteMembreRequestDto;
import com.ensao.gestionprojet.dto.DisponibiliteMembreResponseDto;
import com.ensao.gestionprojet.dto.SprintResponseDto;
import com.ensao.gestionprojet.dto.TacheResponseDto;
import com.ensao.gestionprojet.dto.VelocityDto;
import com.ensao.gestionprojet.entity.BurndownSnapshot;
import com.ensao.gestionprojet.entity.DisponibiliteMembre;
import com.ensao.gestionprojet.entity.Projet;
import com.ensao.gestionprojet.entity.Sprint;
import com.ensao.gestionprojet.entity.Tache;
import com.ensao.gestionprojet.entity.Utilisateur;
import com.ensao.gestionprojet.enums.StatutInvitation;
import com.ensao.gestionprojet.enums.StatutProjet;
import com.ensao.gestionprojet.enums.StatutSprint;
import com.ensao.gestionprojet.enums.StatutTache;
import com.ensao.gestionprojet.helpers.AuthHelper;
import com.ensao.gestionprojet.helpers.ProjectAccessHelper;
import com.ensao.gestionprojet.mapper.SprintMapper;
import com.ensao.gestionprojet.repository.BurndownSnapshotRepository;
import com.ensao.gestionprojet.repository.DisponibiliteMembreRepository;
import com.ensao.gestionprojet.repository.MembreProjetRepository;
import com.ensao.gestionprojet.repository.ProjetRepository;
import com.ensao.gestionprojet.repository.SprintRepository;
import com.ensao.gestionprojet.repository.TacheRepository;
import com.ensao.gestionprojet.repository.UtilisateurRepo;
import com.ensao.gestionprojet.service.SprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final BurndownSnapshotRepository burndownSnapshotRepository;
    private final ProjectAccessHelper projectAccessHelper;

    @Override
    @Transactional
    public SprintResponseDto creerSprint(CreateSprintRequestDto request) {

        Utilisateur createur = authHelper.getUtilisateurCourant();

        Projet projet = projetRepository.findById(request.getProjetId())
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));

        if (projet.getStatut() != StatutProjet.ACTIVE) {
            throw new RuntimeException("Impossible de creer un sprint - le projet n'est pas actif");
        }

        projectAccessHelper.requireManager(createur, projet);

        if (!request.getDateFin().isAfter(request.getDateDebut())) {
            throw new RuntimeException("La date de fin doit etre strictement superieure a la date de debut");
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

    @Override
    @Transactional
    public SprintResponseDto ajouterTaches(Long sprintId, AddTachesSprintRequestDto request) {

        Utilisateur manager = authHelper.getUtilisateurCourant();

        Sprint sprint = getSprintOrThrow(sprintId);
        Projet projet = sprint.getProjet();

        projectAccessHelper.requireManager(manager, projet);

        if (sprint.getStatut() != StatutSprint.PLANNED) {
            throw new RuntimeException("Impossible d'ajouter des taches - le sprint est deja active ou cloture");
        }

        for (Long tacheId : request.getTacheIds()) {
            Tache tache = tacheRepository.findById(tacheId)
                    .orElseThrow(() -> new RuntimeException("Tache introuvable avec l'ID : " + tacheId));

            if (!tache.getProjet().getId().equals(projet.getId())) {
                throw new RuntimeException("La tache " + tache.getTitre() + " n'appartient pas a ce projet");
            }

            if (tache.getStatut() != StatutTache.TODO) {
                throw new RuntimeException("Seules les taches au statut TODO peuvent etre ajoutees a un sprint");
            }

            tache.setSprint(sprint);
            tacheRepository.save(tache);
        }

        return sprintMapper.toDto(sprint);
    }

    @Override
    @Transactional
    public void definirDisponibilite(Long sprintId, List<DisponibiliteMembreRequestDto> request) {

        Utilisateur manager = authHelper.getUtilisateurCourant();

        Sprint sprint = getSprintOrThrow(sprintId);
        Projet projet = sprint.getProjet();

        projectAccessHelper.requireManager(manager, projet);

        if (sprint.getStatut() != StatutSprint.PLANNED) {
            throw new RuntimeException("Impossible de modifier les disponibilites - le sprint est deja active ou cloture");
        }

        for (DisponibiliteMembreRequestDto item : request) {
            Utilisateur targetUser = utilisateurRepository.findById(item.getUtilisateurId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec l'ID : " + item.getUtilisateurId()));

            if (projectAccessHelper.resolveRole(targetUser, projet).isEmpty()) {
                throw new RuntimeException("L'utilisateur " + targetUser.getPrenom() + " " + targetUser.getNom() + " n'est pas membre actif de ce projet");
            }

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

    @Override
    @Transactional(readOnly = true)
    public SprintResponseDto getSprintById(Long sprintId) {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();
        Sprint sprint = getSprintOrThrow(sprintId);

        projectAccessHelper.requireAccess(utilisateur, sprint.getProjet());

        List<TacheResponseDto> taches = sprint.getTaches()
                .stream()
                .map(this::toTacheDto)
                .toList();

        return sprintMapper.toDtoWithTasks(sprint, taches);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisponibiliteMembreResponseDto> getDisponibilites(Long sprintId) {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();
        Sprint sprint = getSprintOrThrow(sprintId);

        projectAccessHelper.requireAccess(utilisateur, sprint.getProjet());

        return disponibiliteMembreRepository.findBySprintId(sprintId)
                .stream()
                .map(this::toDisponibiliteDto)
                .toList();
    }

    @Override
    public List<SprintResponseDto> getSprintsProjet(Long projetId) {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));

        projectAccessHelper.requireAccess(utilisateur, projet);

        return sprintRepository.findByProjetId(projetId)
                .stream()
                .map(sprintMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void activerSprint(Long sprintId) {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();
        Sprint sprint = getSprintOrThrow(sprintId);
        Projet projet = sprint.getProjet();

        projectAccessHelper.requireManager(utilisateur, projet);

        if (sprint.getStatut() != StatutSprint.PLANNED) {
            throw new RuntimeException("Seuls les sprints PLANNED peuvent etre actives");
        }

        boolean activeSprintExists =
                sprintRepository.existsByProjetIdAndStatut(projet.getId(), StatutSprint.ACTIVE);

        if (activeSprintExists) {
            throw new RuntimeException("Un sprint actif existe deja pour ce projet");
        }

        sprint.setStatut(StatutSprint.ACTIVE);
        enregistrerSnapshot(sprint);
        sprintRepository.save(sprint);
    }

    @Override
    @Transactional(readOnly = true)
    public SprintResponseDto getSprintActif(Long projetId) {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));

        projectAccessHelper.requireAccess(utilisateur, projet);

        Sprint sprint = sprintRepository
                .findByProjetIdAndStatut(projetId, StatutSprint.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Aucun sprint actif"));

        List<TacheResponseDto> taches = sprint.getTaches()
                .stream()
                .map(this::toTacheDto)
                .toList();

        return sprintMapper.toDtoWithTasks(sprint, taches);
    }

    @Override
    @Transactional
    public void cloturerSprint(Long sprintId) {

        Utilisateur user = authHelper.getUtilisateurCourant();
        Sprint sprint = getSprintOrThrow(sprintId);
        Projet projet = sprint.getProjet();

        projectAccessHelper.requireManager(user, projet);

        if (sprint.getStatut() != StatutSprint.ACTIVE) {
            throw new RuntimeException("Seul un sprint ACTIVE peut etre cloture");
        }

        int velocity = sprint.getTaches()
                .stream()
                .filter(t -> t.getStatut() == StatutTache.DONE)
                .mapToInt(t -> t.getStoryPoints() == null ? 0 : t.getStoryPoints())
                .sum();

        sprint.setVelocite(velocity);
        sprint.setStatut(StatutSprint.COMPLETED);

        enregistrerSnapshot(sprint);
        sprintRepository.save(sprint);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BurndownDto> getBurndownChart(Long sprintId) {

        Utilisateur user = authHelper.getUtilisateurCourant();
        Sprint sprint = getSprintOrThrow(sprintId);

        projectAccessHelper.requireAccess(user, sprint.getProjet());

        return burndownSnapshotRepository.findBySprintIdOrderByDateAsc(sprintId)
                .stream()
                .map(s -> new BurndownDto(s.getDate(), s.getRemainingPoints()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VelocityDto> getHistoriqueVelocity(Long projetId) {

        Utilisateur manager = authHelper.getUtilisateurCourant();

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));

        projectAccessHelper.requireManager(manager, projet);

        return sprintRepository
                .findByProjetId(projetId)
                .stream()
                .filter(sprint -> sprint.getStatut() == StatutSprint.COMPLETED)
                .map(sprint ->
                        VelocityDto.builder()
                                .sprintId(sprint.getId())
                                .sprintNom(sprint.getNom())
                                .velocite(sprint.getVelocite())
                                .build()
                )
                .toList();
    }

    private Sprint getSprintOrThrow(Long sprintId) {
        return sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint introuvable"));
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

    private TacheResponseDto toTacheDto(Tache tache) {
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
                .priorite(tache.getPriorite() != null ? tache.getPriorite().name() : null)
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

    private DisponibiliteMembreResponseDto toDisponibiliteDto(DisponibiliteMembre disponibilite) {
        Utilisateur utilisateur = disponibilite.getUtilisateur();
        String utilisateurNom = utilisateur.getPrenom() + " " + utilisateur.getNom();

        return DisponibiliteMembreResponseDto.builder()
                .id(disponibilite.getId())
                .sprintId(disponibilite.getSprint().getId())
                .utilisateurId(utilisateur.getId())
                .utilisateurNom(utilisateurNom.trim())
                .heuresDisponibles(disponibilite.getHeuresDisponibles())
                .build();
    }
}
