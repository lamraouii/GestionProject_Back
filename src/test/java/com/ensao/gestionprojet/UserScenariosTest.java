package com.ensao.gestionprojet;

import com.ensao.gestionprojet.entity.*;
import com.ensao.gestionprojet.enums.*;
import com.ensao.gestionprojet.repository.*;
import com.ensao.gestionprojet.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
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
public class UserScenariosTest {

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
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Utilisateur userManager;   // User A (Project Creator, Manager)
    private Utilisateur userAdmin;     // User B (Company Admin)
    private Utilisateur userMember;    // User C (Company Member, added to project)
    private Utilisateur userExternal;  // User D (External User)

    private Entreprise entreprise;

    private String tokenManager;
    private String tokenAdmin;
    private String tokenMember;
    private String tokenExternal;

    @BeforeEach
    public void setUp() {
        // Clear previous database data
        tacheRepository.deleteAll();
        membreProjetRepository.deleteAll();
        projetRepository.deleteAll();
        membreEntrepriseRepository.deleteAll();
        entrepriseRepository.deleteAll();
        utilisateurRepository.deleteAll();

        // 1. Create Users
        userManager = createAndSaveUser("manager@test.com", "Manager", "Test");
        userAdmin = createAndSaveUser("admin@test.com", "Admin", "Test");
        userMember = createAndSaveUser("member@test.com", "Member", "Test");
        userExternal = createAndSaveUser("external@test.com", "External", "Test");

        // Generate tokens
        tokenManager = "Bearer " + jwtService.generateToken(userManager.getEmail());
        tokenAdmin = "Bearer " + jwtService.generateToken(userAdmin.getEmail());
        tokenMember = "Bearer " + jwtService.generateToken(userMember.getEmail());
        tokenExternal = "Bearer " + jwtService.generateToken(userExternal.getEmail());

        // 2. Create Company where userAdmin is creator and ADMIN
        entreprise = Entreprise.builder()
                .nom("Test Company")
                .description("A company for testing")
                .createur(userAdmin)
                .build();
        entreprise = entrepriseRepository.save(entreprise);

        // Add userAdmin as ADMIN in the company
        saveCompanyMember(userAdmin, entreprise, RoleEntreprise.ADMIN, StatutInvitation.ACCEPTED);

        // Add userManager and userMember as normal MEMBERS (ACCEPTED) in the company
        saveCompanyMember(userManager, entreprise, RoleEntreprise.MEMBER, StatutInvitation.ACCEPTED);
        saveCompanyMember(userMember, entreprise, RoleEntreprise.MEMBER, StatutInvitation.ACCEPTED);
        
        // userExternal is NOT added to the company
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

    private void saveCompanyMember(Utilisateur user, Entreprise ent, RoleEntreprise role, StatutInvitation status) {
        MembreEntreprise member = MembreEntreprise.builder()
                .utilisateur(user)
                .entreprise(ent)
                .role(role)
                .statut(status)
                .dateInvitation(LocalDateTime.now())
                .build();
        membreEntrepriseRepository.save(member);
    }

    // =========================================================================
    // Scenario 1: Créer projet personnel → devient ACTIVE
    // =========================================================================
    @Test
    public void testCreatePersonalProjectActiveImmediately() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nom", "Mon Projet Personnel");
        body.put("description", "Description personnelle");
        body.put("type", "PERSONAL");
        body.put("dateDebut", LocalDate.now().toString());
        body.put("dateFin", LocalDate.now().plusMonths(3).toString());

        mockMvc.perform(post("/api/projets")
                        .header("Authorization", tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Mon Projet Personnel"))
                .andExpect(jsonPath("$.type").value("PERSONAL"))
                .andExpect(jsonPath("$.statut").value("ACTIVE"))
                .andExpect(jsonPath("$.entrepriseId").value((Object) null));

        // Verify that the creator is added as MANAGER in the project
        List<Projet> projects = projetRepository.findByCreateurId(userManager.getId());
        assertEquals(1, projects.size());
        Projet createdProj = projects.getFirst();
        assertEquals(StatutProjet.ACTIVE, createdProj.getStatut());

        MembreProjet memberProj = membreProjetRepository
                .findByUtilisateurIdAndProjetId(userManager.getId(), createdProj.getId())
                .orElse(null);
        assertNotNull(memberProj);
        assertEquals(RoleProjet.MANAGER, memberProj.getRole());
        assertEquals(StatutInvitation.ACCEPTED, memberProj.getStatut());
    }

    // =========================================================================
    // Scenario 2: Créer projet entreprise → devient PENDING
    // =========================================================================
    @Test
    public void testCreateCompanyProjectPendingApproval() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nom", "Projet de l'entreprise");
        body.put("description", "Description entreprise");
        body.put("type", "ENTREPRISE");
        body.put("entrepriseId", entreprise.getId());
        body.put("dateDebut", LocalDate.now().toString());
        body.put("dateFin", LocalDate.now().plusMonths(3).toString());

        mockMvc.perform(post("/api/projets")
                        .header("Authorization", tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Projet de l'entreprise"))
                .andExpect(jsonPath("$.type").value("ENTREPRISE"))
                .andExpect(jsonPath("$.statut").value("PENDING"))
                .andExpect(jsonPath("$.entrepriseId").value(entreprise.getId()));

        Projet createdProj = projetRepository.findAll().stream()
                .filter(p -> p.getType() == TypeProjet.ENTREPRISE)
                .findFirst()
                .orElse(null);
        assertNotNull(createdProj);
        assertEquals(StatutProjet.PENDING, createdProj.getStatut());
    }

    // =========================================================================
    // Scenario 3 & 7: Admin valide → devient ACTIVE & non-admin ne peut pas valider
    // =========================================================================
    @Test
    public void testValidationAndPermissionsOnCompanyProject() throws Exception {
        // 1. Setup a pending company project in DB
        Projet proj = Projet.builder()
                .nom("Projet Secret")
                .type(TypeProjet.ENTREPRISE)
                .entreprise(entreprise)
                .createur(userManager)
                .statut(StatutProjet.PENDING)
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusDays(30))
                .build();
        final Projet finalProj = projetRepository.save(proj);

        // Add creator as MANAGER
        membreProjetRepository.save(MembreProjet.builder()
                .utilisateur(userManager)
                .projet(finalProj)
                .role(RoleProjet.MANAGER)
                .statut(StatutInvitation.ACCEPTED)
                .dateAdhesion(LocalDateTime.now())
                .build());

        // 2. Scenario 7: A non-admin (like userMember) attempts to validate.
        // It should throw an exception/fail.
        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(put("/api/projets/" + finalProj.getId() + "/valider")
                            .header("Authorization", tokenMember));
        });
        assertTrue(exception.getCause().getMessage().contains("Seul l'ADMIN de l'entreprise peut effectuer cette action"));

        // Verify status remains PENDING in DB
        assertEquals(StatutProjet.PENDING, projetRepository.findById(finalProj.getId()).get().getStatut());

        // 3. Scenario 3: The company admin validates it.
        mockMvc.perform(put("/api/projets/" + finalProj.getId() + "/valider")
                        .header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ACTIVE"));

        // Verify status is ACTIVE in DB
        assertEquals(StatutProjet.ACTIVE, projetRepository.findById(finalProj.getId()).get().getStatut());
    }

    // =========================================================================
    // Scenario 4 & 7: Ajouter membres & utilisateur externe ne peut pas rejoindre
    // =========================================================================
    @Test
    public void testInviteMembersAndCompanyConstraints() throws Exception {
        // Setup an active company project
        Projet proj = Projet.builder()
                .nom("Projet Innovant")
                .type(TypeProjet.ENTREPRISE)
                .entreprise(entreprise)
                .createur(userManager)
                .statut(StatutProjet.ACTIVE)
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusDays(30))
                .build();
        final Projet finalProj = projetRepository.save(proj);

        // Add creator as MANAGER
        membreProjetRepository.save(MembreProjet.builder()
                .utilisateur(userManager)
                .projet(finalProj)
                .role(RoleProjet.MANAGER)
                .statut(StatutInvitation.ACCEPTED)
                .dateAdhesion(LocalDateTime.now())
                .build());

        // 1. Scenario 4: Manager invites User C (userMember - who is in the company)
        Map<String, Object> inviteBody = new HashMap<>();
        inviteBody.put("email", userMember.getEmail());

        mockMvc.perform(post("/api/projets/" + finalProj.getId() + "/invite")
                        .header("Authorization", tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inviteBody)))
                .andExpect(status().isOk());

        // Check C is added
        assertTrue(membreProjetRepository.findByUtilisateurIdAndProjetId(userMember.getId(), finalProj.getId()).isPresent());

        // 2. Scenario 7: Manager tries to invite User D (userExternal - NOT in the company)
        Map<String, Object> externalInviteBody = new HashMap<>();
        externalInviteBody.put("email", userExternal.getEmail());

        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/api/projets/" + finalProj.getId() + "/invite")
                            .header("Authorization", tokenManager)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(externalInviteBody)));
        });
        assertTrue(exception.getCause().getMessage().contains("Pour un projet d'entreprise, l'invité doit être membre accepté de l'entreprise"));

        // Check D is not added
        assertFalse(membreProjetRepository.findByUtilisateurIdAndProjetId(userExternal.getId(), finalProj.getId()).isPresent());
    }

    // =========================================================================
    // Scenario 5, 6 & 7: Créer tâche, assigner tâche & assigner quelqu'un hors projet
    // =========================================================================
    @Test
    public void testTaskCreationAndAssignment() throws Exception {
        // Setup an active company project
        Projet proj = Projet.builder()
                .nom("Projet Kanban")
                .type(TypeProjet.ENTREPRISE)
                .entreprise(entreprise)
                .createur(userManager)
                .statut(StatutProjet.ACTIVE)
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusDays(30))
                .build();
        proj = projetRepository.save(proj);

        // Add creator as MANAGER
        membreProjetRepository.save(MembreProjet.builder()
                .utilisateur(userManager)
                .projet(proj)
                .role(RoleProjet.MANAGER)
                .statut(StatutInvitation.ACCEPTED)
                .dateAdhesion(LocalDateTime.now())
                .build());

        // Add User C to project members
        membreProjetRepository.save(MembreProjet.builder()
                .utilisateur(userMember)
                .projet(proj)
                .role(RoleProjet.MEMBER)
                .statut(StatutInvitation.ACCEPTED)
                .dateAdhesion(LocalDateTime.now())
                .build());

        // 1. Scenario 5: Create a task in this project
        Map<String, Object> taskBody = new HashMap<>();
        taskBody.put("titre", "Implémenter la validation");
        taskBody.put("description", "Finir l'écriture des tests d'intégration");
        taskBody.put("priorite", "HIGH");
        taskBody.put("projetId", proj.getId());
        taskBody.put("storyPoints", 5);

        String taskResponseString = mockMvc.perform(post("/api/taches")
                        .header("Authorization", tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titre").value("Implémenter la validation"))
                .andExpect(jsonPath("$.statut").value("TODO"))
                .andExpect(jsonPath("$.priorite").value("HIGH"))
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> taskResponse = objectMapper.readValue(taskResponseString, Map.class);
        Long taskId = ((Number) taskResponse.get("id")).longValue();

        // 2. Scenario 6: Assign task to User C (userMember, who is in the project)
        mockMvc.perform(put("/api/taches/" + taskId + "/assigner/" + userMember.getId())
                        .header("Authorization", tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utilisateurAssigneId").value(userMember.getId()))
                .andExpect(jsonPath("$.utilisateurAssigneNom").value("Member Test"));

        // 3. Scenario 7: Try to assign the task to User D (userExternal, who is NOT in the project)
        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(put("/api/taches/" + taskId + "/assigner/" + userExternal.getId())
                            .header("Authorization", tokenManager));
        });
        assertTrue(exception.getCause().getMessage().contains("L'utilisateur doit être membre accepté du projet pour être assigné"));
    }
}
