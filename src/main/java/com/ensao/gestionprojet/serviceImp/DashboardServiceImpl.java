package com.ensao.gestionprojet.serviceImp;

import com.ensao.gestionprojet.dto.MetriquesDto;
import com.ensao.gestionprojet.dto.WorkloadDto;
import com.ensao.gestionprojet.entity.MembreEntreprise;
import com.ensao.gestionprojet.entity.MembreProjet;
import com.ensao.gestionprojet.entity.Projet;
import com.ensao.gestionprojet.entity.Tache;
import com.ensao.gestionprojet.entity.Utilisateur;
import com.ensao.gestionprojet.enums.RoleEntreprise;
import com.ensao.gestionprojet.enums.StatutInvitation;
import com.ensao.gestionprojet.enums.StatutTache;
import com.ensao.gestionprojet.enums.TypeProjet;
import com.ensao.gestionprojet.helpers.AuthHelper;
import com.ensao.gestionprojet.helpers.ProjectAccessHelper;
import com.ensao.gestionprojet.repository.MembreEntrepriseRepository;
import com.ensao.gestionprojet.repository.MembreProjetRepository;
import com.ensao.gestionprojet.repository.ProjetRepository;
import com.ensao.gestionprojet.repository.TacheRepository;
import com.ensao.gestionprojet.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final TacheRepository tacheRepository;
    private final MembreProjetRepository membreProjetRepository;
    private final MembreEntrepriseRepository membreEntrepriseRepository;
    private final ProjetRepository projetRepository;
    private final AuthHelper authHelper;
    private final ProjectAccessHelper projectAccessHelper;

    @Override
    public List<WorkloadDto> getWorkload(Long projetId) {

        Utilisateur currentUser = authHelper.getUtilisateurCourant();
        Projet projet = getProjetOrThrow(projetId);

        projectAccessHelper.requireAccess(currentUser, projet);

        List<Utilisateur> membres = getProjectUsers(projet);
        List<WorkloadDto> response = new ArrayList<>();

        for (Utilisateur user : membres) {
            List<Tache> taches = tacheRepository.findByProjetIdAndUtilisateurAssigneId(projetId, user.getId());

            int todo = 0;
            int inProgress = 0;
            int done = 0;

            for (Tache t : taches) {
                if (t.getStatut() == StatutTache.TODO) {
                    todo++;
                } else if (t.getStatut() == StatutTache.IN_PROGRESS) {
                    inProgress++;
                } else if (t.getStatut() == StatutTache.DONE) {
                    done++;
                }
            }

            response.add(WorkloadDto.builder()
                    .utilisateurId(user.getId())
                    .utilisateurNom(user.getPrenom() + " " + user.getNom())
                    .tachesTodo(todo)
                    .tachesInProgress(inProgress)
                    .tachesDone(done)
                    .totalTaches(taches.size())
                    .build());
        }

        return response;
    }

    @Override
    public MetriquesDto getMetriques(Long projetId) {

        Utilisateur currentUser = authHelper.getUtilisateurCourant();
        Projet projet = getProjetOrThrow(projetId);

        projectAccessHelper.requireAccess(currentUser, projet);

        List<Tache> doneTasks = tacheRepository.findByProjetIdAndStatut(projetId, StatutTache.DONE);

        double totalLeadTimeHours = 0.0;
        int leadTimeCount = 0;

        double totalCycleTimeHours = 0.0;
        int cycleTimeCount = 0;

        for (Tache t : doneTasks) {
            if (t.getDateCompletion() != null && t.getDateCreation() != null) {
                long seconds = Duration.between(t.getDateCreation(), t.getDateCompletion()).toSeconds();
                totalLeadTimeHours += (double) seconds / 3600.0;
                leadTimeCount++;
            }

            if (t.getDateCompletion() != null && t.getDateDebutTravail() != null) {
                long seconds = Duration.between(t.getDateDebutTravail(), t.getDateCompletion()).toSeconds();
                totalCycleTimeHours += (double) seconds / 3600.0;
                cycleTimeCount++;
            }
        }

        double avgLeadTime = leadTimeCount > 0 ? (totalLeadTimeHours / leadTimeCount) : 0.0;
        double avgCycleTime = cycleTimeCount > 0 ? (totalCycleTimeHours / cycleTimeCount) : 0.0;

        avgLeadTime = Math.round(avgLeadTime * 100.0) / 100.0;
        avgCycleTime = Math.round(avgCycleTime * 100.0) / 100.0;

        return MetriquesDto.builder()
                .leadTimeMoyenHeures(avgLeadTime)
                .cycleTimeMoyenHeures(avgCycleTime)
                .build();
    }

    private Projet getProjetOrThrow(Long projetId) {
        return projetRepository.findById(projetId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));
    }

    private List<Utilisateur> getProjectUsers(Projet projet) {
        List<Utilisateur> users = new ArrayList<>();
        Set<Long> userIds = new HashSet<>();

        membreProjetRepository
                .findByProjetIdAndStatut(projet.getId(), StatutInvitation.ACCEPTED)
                .stream()
                .map(MembreProjet::getUtilisateur)
                .forEach(user -> {
                    users.add(user);
                    userIds.add(user.getId());
                });

        if (projet.getType() == TypeProjet.ENTREPRISE && projet.getEntreprise() != null) {
            membreEntrepriseRepository
                    .findByEntrepriseIdAndStatut(projet.getEntreprise().getId(), StatutInvitation.ACCEPTED)
                    .stream()
                    .filter(membre -> membre.getRole() == RoleEntreprise.ADMIN)
                    .map(MembreEntreprise::getUtilisateur)
                    .filter(user -> !userIds.contains(user.getId()))
                    .forEach(users::add);
        }

        return users;
    }
}
