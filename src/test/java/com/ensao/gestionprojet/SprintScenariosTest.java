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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SprintScenariosTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
    private SprintRepository sprintRepository;

    @Autowired
    private DisponibiliteMembreRepository disponibiliteMembreRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        disponibiliteMembreRepository.deleteAll();
        sprintRepository.deleteAll();
        membreProjetRepository.deleteAll();
        projetRepository.deleteAll();
        membreEntrepriseRepository.deleteAll();
        entrepriseRepository.deleteAll();
        utilisateurRepository.deleteAll();

        // Setup users
        userManager = createAndSaveUser("manager@test.com", "Manager", "Test");
        userMember = createAndSaveUser("member@test.com", "Member", "Test");
        userExternal = createAndSaveUser("external@test.com", "External", "Test");

        tokenManager = "Bearer " + jwtService.generateToken(userManager.getEmail());
        tokenMember = "Bearer " + jwtService.generateToken(userMember.getEmail());
        tokenExternal = "Bearer " + jwtService.generateToken(userExternal.getEmail());

        // Create active project
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
    // US13 — Créer un sprint (MANAGER)
    // =========================================================================
    @Test
    public void testCreateSprint() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nom", "Sprint 1");
        body.put("objectif", "Finir l'implémentation de la validation");
        body.put("dateDebut", LocalDate.now().toString());
        body.put("dateFin", LocalDate.now().plusDays(14).toString());
        body.put("projetId", activeProject.getId());

        // 1. Success creating as manager
        mockMvc.perform(post("/api/sprints")
                        .header("Authorization", tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Sprint 1"))
                .andExpect(jsonPath("$.statut").value("PLANNED"));

        // 2. Fail when user is member but not manager
        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/api/sprints")
                            .header("Authorization", tokenMember)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)));
        });

        // 3. Fail when dateFin <= dateDebut
        body.put("dateFin", LocalDate.now().toString());
        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/api/sprints")
                            .header("Authorization", tokenManager)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)));
        });
    }

    // =========================================================================
    // US14 — Ajouter des tâches au sprint (MANAGER)
    // =========================================================================
    @Test
    public void testAddTasksToSprint() throws Exception {
        // Setup sprint
        Sprint sprint = Sprint.builder()
                .projet(activeProject)
                .nom("Sprint Alpha")
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusDays(14))
                .statut(StatutSprint.PLANNED)
                .createur(userManager)
                .build();
        sprint = sprintRepository.save(sprint);

        // Setup TODO task
        Tache todoTask = Tache.builder()
                .titre("Todo task")
                .projet(activeProject)
                .statut(StatutTache.TODO)
                .createur(userManager)
                .build();
        todoTask = tacheRepository.save(todoTask);

        // Setup IN_PROGRESS task
        Tache inProgressTask = Tache.builder()
                .titre("In Progress task")
                .projet(activeProject)
                .statut(StatutTache.IN_PROGRESS)
                .createur(userManager)
                .build();
        inProgressTask = tacheRepository.save(inProgressTask);

        final Long sprintId = sprint.getId();
        final Long todoTaskId = todoTask.getId();
        final Long ipTaskId = inProgressTask.getId();

        // 1. Success adding TODO task to sprint
        Map<String, Object> body = new HashMap<>();
        body.put("tacheIds", Collections.singletonList(todoTaskId));

        mockMvc.perform(post("/api/sprints/" + sprintId + "/taches")
                        .header("Authorization", tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // Verify task has sprint assigned in DB
        Tache updatedTodoTask = tacheRepository.findById(todoTaskId).get();
        assertEquals(sprintId, updatedTodoTask.getSprint().getId());

        // 2. Fail when adding IN_PROGRESS task to sprint
        Map<String, Object> ipBody = new HashMap<>();
        ipBody.put("tacheIds", Collections.singletonList(ipTaskId));

        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/api/sprints/" + sprintId + "/taches")
                            .header("Authorization", tokenManager)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ipBody)));
        });

        // 3. Fail when non-manager tries to add task
        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/api/sprints/" + sprintId + "/taches")
                            .header("Authorization", tokenMember)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)));
        });
    }

    // =========================================================================
    // US15 — Définir la disponibilité des membres (MANAGER)
    // =========================================================================
    @Test
    public void testDefineMemberAvailability() throws Exception {
        // Setup sprint
        Sprint sprint = Sprint.builder()
                .projet(activeProject)
                .nom("Sprint Beta")
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusDays(14))
                .statut(StatutSprint.PLANNED)
                .createur(userManager)
                .build();
        sprint = sprintRepository.save(sprint);

        final Long sprintId = sprint.getId();

        // 1. Success setting availability for userMember (who is in the project)
        Map<String, Object> avail1 = new HashMap<>();
        avail1.put("utilisateurId", userMember.getId());
        avail1.put("heuresDisponibles", 40);

        mockMvc.perform(post("/api/sprints/" + sprintId + "/disponibilites")
                        .header("Authorization", tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(avail1))))
                .andExpect(status().isOk());

        // Verify saved in DB
        DisponibiliteMembre disp = disponibiliteMembreRepository
                .findBySprintIdAndUtilisateurId(sprintId, userMember.getId())
                .orElse(null);
        assertNotNull(disp);
        assertEquals(40, disp.getHeuresDisponibles());

        // 2. Fail when defining for external user (not in project)
        Map<String, Object> availExt = new HashMap<>();
        availExt.put("utilisateurId", userExternal.getId());
        availExt.put("heuresDisponibles", 35);

        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/api/sprints/" + sprintId + "/disponibilites")
                            .header("Authorization", tokenManager)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Collections.singletonList(availExt))));
        });

        // 3. Fail when non-manager tries to set availability
        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/api/sprints/" + sprintId + "/disponibilites")
                            .header("Authorization", tokenMember)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Collections.singletonList(avail1))));
        });
    }
}
