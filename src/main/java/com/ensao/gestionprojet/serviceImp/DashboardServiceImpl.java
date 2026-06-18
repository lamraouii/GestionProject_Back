package com.ensao.gestionprojet.serviceImp;

import com.ensao.gestionprojet.dto.MetriquesDto;
import com.ensao.gestionprojet.dto.WorkloadDto;
import com.ensao.gestionprojet.entity.MembreProjet;
import com.ensao.gestionprojet.entity.Tache;
import com.ensao.gestionprojet.entity.Utilisateur;
import com.ensao.gestionprojet.enums.StatutInvitation;
import com.ensao.gestionprojet.enums.StatutTache;
import com.ensao.gestionprojet.repository.MembreProjetRepository;
import com.ensao.gestionprojet.repository.TacheRepository;
import com.ensao.gestionprojet.service.AuthHelper;
import com.ensao.gestionprojet.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final TacheRepository tacheRepository;
    private final MembreProjetRepository membreProjetRepository;
    private final AuthHelper authHelper;

    // ============================================================
    //  US21 — Obtenir le workload des membres du projet
    // ============================================================
    @Override
    public List<WorkloadDto> getWorkload(Long projetId) {

        Utilisateur currentUser = authHelper.getUtilisateurCourant();

        // Vérifier que l'utilisateur courant est membre accepté du projet
        membreProjetRepository.findByUtilisateurIdAndProjetIdAndStatut(
                currentUser.getId(), projetId, StatutInvitation.ACCEPTED)
                .orElseThrow(() -> new RuntimeException("Accès refusé — vous n'êtes pas membre de ce projet"));

        // Récupérer les membres acceptés du projet
        List<MembreProjet> membres = membreProjetRepository.findByProjetIdAndStatut(projetId, StatutInvitation.ACCEPTED);

        List<WorkloadDto> response = new ArrayList<>();

        for (MembreProjet mp : membres) {
            Utilisateur user = mp.getUtilisateur();

            // Récupérer toutes les tâches assignées à ce membre dans ce projet
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

    // ============================================================
    //  US22 — Obtenir les métriques (Lead/Cycle Time)
    // ============================================================
    @Override
    public MetriquesDto getMetriques(Long projetId) {

        Utilisateur currentUser = authHelper.getUtilisateurCourant();

        // Vérifier que l'utilisateur courant est membre accepté du projet
        membreProjetRepository.findByUtilisateurIdAndProjetIdAndStatut(
                currentUser.getId(), projetId, StatutInvitation.ACCEPTED)
                .orElseThrow(() -> new RuntimeException("Accès refusé — vous n'êtes pas membre de ce projet"));

        // Récupérer les tâches DONE du projet
        List<Tache> doneTasks = tacheRepository.findByProjetIdAndStatut(projetId, StatutTache.DONE);

        double totalLeadTimeHours = 0.0;
        int leadTimeCount = 0;

        double totalCycleTimeHours = 0.0;
        int cycleTimeCount = 0;

        for (Tache t : doneTasks) {
            // Lead Time = dateCompletion - dateCreation
            if (t.getDateCompletion() != null && t.getDateCreation() != null) {
                long seconds = Duration.between(t.getDateCreation(), t.getDateCompletion()).toSeconds();
                totalLeadTimeHours += (double) seconds / 3600.0;
                leadTimeCount++;
            }

            // Cycle Time = dateCompletion - dateDebutTravail
            if (t.getDateCompletion() != null && t.getDateDebutTravail() != null) {
                long seconds = Duration.between(t.getDateDebutTravail(), t.getDateCompletion()).toSeconds();
                totalCycleTimeHours += (double) seconds / 3600.0;
                cycleTimeCount++;
            }
        }

        double avgLeadTime = leadTimeCount > 0 ? (totalLeadTimeHours / leadTimeCount) : 0.0;
        double avgCycleTime = cycleTimeCount > 0 ? (totalCycleTimeHours / cycleTimeCount) : 0.0;

        // Formater les moyennes à 2 décimales
        avgLeadTime = Math.round(avgLeadTime * 100.0) / 100.0;
        avgCycleTime = Math.round(avgCycleTime * 100.0) / 100.0;

        return MetriquesDto.builder()
                .leadTimeMoyenHeures(avgLeadTime)
                .cycleTimeMoyenHeures(avgCycleTime)
                .build();
    }
}
