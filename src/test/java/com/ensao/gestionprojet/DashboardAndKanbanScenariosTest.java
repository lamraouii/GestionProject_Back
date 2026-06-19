package com.ensao.gestionprojet;

import com.ensao.gestionprojet.entity.*;
import com.ensao.gestionprojet.enums.*;
import com.ensao.gestionprojet.repository.*;
import com.ensao.gestionprojet.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class DashboardAndKanbanScenariosTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UtilisateurRepo utilisateurRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @Autowired
    private MembreEntrepriseRepository membreEntrepriseRepository;

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private MembreProjetRepository membreProjetRepository;

    @Autowired
    private TacheRepository tacheRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Utilisateur userManager;
    private Utilisateur userMember;
    private Utilisateur userExternal;

    private Projet activeProject;

    private String tokenManager;
    private String tokenMember;
    private String tokenExternal;

    @BeforeEach
    public void setUp() {
        tacheRepository.deleteAll();
        membreProjetRepository.deleteAll();
        projetRepository.deleteAll();
        membreEntrepriseRepository.deleteAll();
        entrepriseRepository.deleteAll();
        utilisateurRepository.deleteAll();

        // Create users
        userManager = createAndSaveUser("manager@test.com", "Manager", "Test");
        userMember = createAndSaveUser("member@test.com", "Member", "Test");
        userExternal = createAndSaveUser("external@test.com", "External", "Test");

        tokenManager = "Bearer " + jwtService.generateToken(userManager.getEmail());
        tokenMember = "Bearer " + jwtService.generateToken(userMember.getEmail());
        tokenExternal = "Bearer " + jwtService.generateToken(userExternal.getEmail());

        // Create project
        activeProject = Projet.builder()
                .nom("Active Project")
                .type(TypeProjet.PERSONAL)
                .statut(StatutProjet.ACTIVE)
                .createur(userManager)
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusDays(30))
                .build();
        activeProject = projetRepository.save(activeProject);

        // Add creator as MANAGER
        membreProjetRepository.save(MembreProjet.builder()
                .utilisateur(userManager)
                .projet(activeProject)
                .role(RoleProjet.MANAGER)
                .statut(StatutInvitation.ACCEPTED)
                .dateAdhesion(LocalDateTime.now())
                .build());

        // Add User C (member) to project members
        membreProjetRepository.save(MembreProjet.builder()
                .utilisateur(userMember)
                .projet(activeProject)
                .role(RoleProjet.MEMBER)
                .statut(StatutInvitation.ACCEPTED)
                .dateAdhesion(LocalDateTime.now())
                .build());
    }

    private Utilisateur createAndSaveUser(String email, String prenom, String nom) {
        Utilisateur user = Utilisateur.builder()
                .email(email)
                .prenom(prenom)
                .nom(nom)
                .motDePasse(passwordEncoder.encode("Password123"))
                .estActif(true)
                .build();
        return utilisateurRepository.save(user);
    }

    // =========================================================================
    // US21 — Obtenir le workload (nombre de tâches par statut) par membre
    // =========================================================================
    @Test
    public void testGetWorkload() throws Exception {
        // Setup tasks
        // Manager tasks: 1 TODO, 1 DONE
        tacheRepository.save(Tache.builder()
                .titre("Task 1")
                .projet(activeProject)
                .statut(StatutTache.TODO)
                .createur(userManager)
                .utilisateurAssigne(userManager)
                .build());

        tacheRepository.save(Tache.builder()
                .titre("Task 2")
                .projet(activeProject)
                .statut(StatutTache.DONE)
                .createur(userManager)
                .utilisateurAssigne(userManager)
                .build());

        // Member tasks: 2 IN_PROGRESS
        tacheRepository.save(Tache.builder()
                .titre("Task 3")
                .projet(activeProject)
                .statut(StatutTache.IN_PROGRESS)
                .createur(userManager)
                .utilisateurAssigne(userMember)
                .build());

        tacheRepository.save(Tache.builder()
                .titre("Task 4")
                .projet(activeProject)
                .statut(StatutTache.IN_PROGRESS)
                .createur(userManager)
                .utilisateurAssigne(userMember)
                .build());

        final Long projId = activeProject.getId();

        // 1. Success requesting as member
        mockMvc.perform(get("/api/dashboard/" + projId + "/workload")
                        .header("Authorization", tokenMember))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.utilisateurId == " + userManager.getId() + ")].tachesTodo").value(1))
                .andExpect(jsonPath("$[?(@.utilisateurId == " + userManager.getId() + ")].tachesDone").value(1))
                .andExpect(jsonPath("$[?(@.utilisateurId == " + userMember.getId() + ")].tachesInProgress").value(2))
                .andExpect(jsonPath("$[?(@.utilisateurId == " + userMember.getId() + ")].totalTaches").value(2));

        // 2. Fail for external user
        assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/api/dashboard/" + projId + "/workload")
                            .header("Authorization", tokenExternal));
        });
    }

    // =========================================================================
    // US22 — Obtenir les métriques Lead & Cycle Time moyens
    // =========================================================================
    @Test
    public void testGetMetriques() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        // Task 1: created 24h ago, started 12h ago, completed now -> Lead = 24h, Cycle = 12h
        Tache t1 = Tache.builder()
                .titre("Task A")
                .projet(activeProject)
                .statut(StatutTache.DONE)
                .createur(userManager)
                .dateDebutTravail(now.minusHours(12))
                .dateCompletion(now)
                .build();
        t1 = tacheRepository.save(t1);
        jdbcTemplate.update("UPDATE task SET date_creation = ? WHERE id = ?", java.sql.Timestamp.valueOf(now.minusHours(24)), t1.getId());

        // Task 2: created 48h ago, started 24h ago, completed now -> Lead = 48h, Cycle = 24h
        Tache t2 = Tache.builder()
                .titre("Task B")
                .projet(activeProject)
                .statut(StatutTache.DONE)
                .createur(userManager)
                .dateDebutTravail(now.minusHours(24))
                .dateCompletion(now)
                .build();
        t2 = tacheRepository.save(t2);
        jdbcTemplate.update("UPDATE task SET date_creation = ? WHERE id = ?", java.sql.Timestamp.valueOf(now.minusHours(48)), t2.getId());

        // Clear hibernate cache to force reload of date_creation
        entityManager.flush();
        entityManager.clear();

        final Long projId = activeProject.getId();

        // 1. Success requesting as member
        mockMvc.perform(get("/api/dashboard/" + projId + "/metriques")
                        .header("Authorization", tokenMember))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leadTimeMoyenHeures").value(36.0))
                .andExpect(jsonPath("$.cycleTimeMoyenHeures").value(18.0));

        // 2. Fail for external user
        assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/api/dashboard/" + projId + "/metriques")
                            .header("Authorization", tokenExternal));
        });
    }

    // =========================================================================
    // US23 — Obtenir la vue Kanban Board (Todo, In Progress, Done)
    // =========================================================================
    @Test
    public void testGetKanbanBoard() throws Exception {
        // Create 1 TODO task
        tacheRepository.save(Tache.builder()
                .titre("Todo task")
                .projet(activeProject)
                .statut(StatutTache.TODO)
                .createur(userManager)
                .build());

        // Create 2 IN_PROGRESS tasks
        tacheRepository.save(Tache.builder()
                .titre("Progress task 1")
                .projet(activeProject)
                .statut(StatutTache.IN_PROGRESS)
                .createur(userManager)
                .build());

        tacheRepository.save(Tache.builder()
                .titre("Progress task 2")
                .projet(activeProject)
                .statut(StatutTache.IN_PROGRESS)
                .createur(userManager)
                .build());

        // Create 1 DONE task
        tacheRepository.save(Tache.builder()
                .titre("Completed task")
                .projet(activeProject)
                .statut(StatutTache.DONE)
                .createur(userManager)
                .build());

        final Long projId = activeProject.getId();

        // 1. Success requesting as member
        mockMvc.perform(get("/api/taches/kanban/" + projId)
                        .header("Authorization", tokenMember))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todo.length()").value(1))
                .andExpect(jsonPath("$.inProgress.length()").value(2))
                .andExpect(jsonPath("$.done.length()").value(1));

        // 2. Fail for external user
        assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/api/taches/kanban/" + projId)
                            .header("Authorization", tokenExternal));
        });
    }
}
