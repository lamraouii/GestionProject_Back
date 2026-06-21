package com.ensao.gestionprojet.serviceImp;

import com.ensao.gestionprojet.dto.CreateProjetRequestDto;
import com.ensao.gestionprojet.dto.InviteMembreProjetRequestDto;
import com.ensao.gestionprojet.dto.MemberResponseDto;
import com.ensao.gestionprojet.dto.ProjetResponseDto;
import com.ensao.gestionprojet.entity.Entreprise;
import com.ensao.gestionprojet.entity.MembreEntreprise;
import com.ensao.gestionprojet.entity.MembreProjet;
import com.ensao.gestionprojet.entity.Projet;
import com.ensao.gestionprojet.entity.Utilisateur;
import com.ensao.gestionprojet.enums.RoleEntreprise;
import com.ensao.gestionprojet.enums.RoleProjet;
import com.ensao.gestionprojet.enums.StatutInvitation;
import com.ensao.gestionprojet.enums.StatutProjet;
import com.ensao.gestionprojet.enums.TypeProjet;
import com.ensao.gestionprojet.helpers.AuthHelper;
import com.ensao.gestionprojet.helpers.ProjectAccessHelper;
import com.ensao.gestionprojet.repository.EntrepriseRepository;
import com.ensao.gestionprojet.repository.MembreEntrepriseRepository;
import com.ensao.gestionprojet.repository.MembreProjetRepository;
import com.ensao.gestionprojet.repository.ProjetRepository;
import com.ensao.gestionprojet.repository.UtilisateurRepo;
import com.ensao.gestionprojet.service.EmailService;
import com.ensao.gestionprojet.service.ProjetService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjetServiceImpl implements ProjetService {

    private final ProjetRepository projetRepository;
    private final MembreProjetRepository membreProjetRepository;
    private final MembreEntrepriseRepository membreEntrepriseRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final UtilisateurRepo utilisateurRepository;
    private final AuthHelper authHelper;
    private final ProjectAccessHelper projectAccessHelper;
    private final EmailService emailService;

    @Override
    @Transactional
    public ProjetResponseDto creerProjet(CreateProjetRequestDto request) {

        Utilisateur createur = authHelper.getUtilisateurCourant();
        TypeProjet type = TypeProjet.valueOf(request.getType().toUpperCase());

        Projet projet = new Projet();
        projet.setNom(request.getNom());
        projet.setDescription(request.getDescription());
        projet.setType(type);
        projet.setCreateur(createur);
        projet.setDateDebut(request.getDateDebut());
        projet.setDateFin(request.getDateFin());

        MembreEntreprise membreEntrepriseCreateur = null;

        if (type == TypeProjet.PERSONAL) {
            projet.setEntreprise(null);
            projet.setStatut(StatutProjet.ACTIVE);
        } else {
            if (request.getEntrepriseId() == null) {
                throw new RuntimeException("L'identifiant de l'entreprise est obligatoire pour un projet d'entreprise");
            }

            Entreprise entreprise = entrepriseRepository.findById(request.getEntrepriseId())
                    .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));

            membreEntrepriseCreateur = membreEntrepriseRepository
                    .findByUtilisateurIdAndEntrepriseId(createur.getId(), entreprise.getId())
                    .filter(m -> m.getStatut() == StatutInvitation.ACCEPTED)
                    .orElseThrow(() -> new RuntimeException(
                            "Vous devez etre membre accepte de l'entreprise pour creer un projet d'entreprise"));

            projet.setEntreprise(entreprise);
            projet.setStatut(
                    membreEntrepriseCreateur.getRole() == RoleEntreprise.ADMIN
                            ? StatutProjet.ACTIVE
                            : StatutProjet.PENDING
            );
        }

        Projet savedProjet = projetRepository.save(projet);

        MembreProjet membreProjet = MembreProjet.builder()
                .utilisateur(createur)
                .projet(savedProjet)
                .role(RoleProjet.MANAGER)
                .statut(StatutInvitation.ACCEPTED)
                .invitePar(createur)
                .dateAdhesion(LocalDateTime.now())
                .build();

        membreProjetRepository.save(membreProjet);

        if (type == TypeProjet.ENTREPRISE && savedProjet.getStatut() == StatutProjet.PENDING) {
            notifierAdminsProjetEnAttente(savedProjet, createur);
        }

        return toDto(savedProjet, RoleProjet.MANAGER);
    }

    @Override
    @Transactional
    public ProjetResponseDto validerProjet(Long projetId) {

        Utilisateur admin = authHelper.getUtilisateurCourant();

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));

        if (projet.getType() != TypeProjet.ENTREPRISE) {
            throw new RuntimeException("Seuls les projets d'entreprise necessitent une validation");
        }
        if (projet.getStatut() != StatutProjet.PENDING) {
            throw new RuntimeException("Ce projet n'est pas en attente de validation");
        }

        verifierAdminEntreprise(admin, projet.getEntreprise().getId());

        projet.setStatut(StatutProjet.ACTIVE);
        Projet savedProjet = projetRepository.save(projet);

        return toDto(savedProjet, RoleProjet.MANAGER);
    }

    @Override
    @Transactional
    public ProjetResponseDto rejeterProjet(Long projetId) {

        Utilisateur admin = authHelper.getUtilisateurCourant();

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));

        if (projet.getType() != TypeProjet.ENTREPRISE) {
            throw new RuntimeException("Seuls les projets d'entreprise necessitent une validation");
        }
        if (projet.getStatut() != StatutProjet.PENDING) {
            throw new RuntimeException("Ce projet n'est pas en attente de validation");
        }

        verifierAdminEntreprise(admin, projet.getEntreprise().getId());

        projet.setStatut(StatutProjet.REJECTED);
        Projet savedProjet = projetRepository.save(projet);

        return toDto(savedProjet, RoleProjet.MANAGER);
    }

    @Override
    public ProjetResponseDto getProjet(Long projetId) {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));

        RoleProjet role = projectAccessHelper.requireAccess(utilisateur, projet);

        return toDto(projet, role);
    }

    @Override
    public List<ProjetResponseDto> getMesProjets() {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();

        List<MembreProjet> memberships = membreProjetRepository
                .findByUtilisateurIdAndStatut(utilisateur.getId(), StatutInvitation.ACCEPTED);

        return memberships.stream()
                .map(m -> toDto(m.getProjet(), m.getRole()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjetResponseDto> getProjetsEntreprise(Long entrepriseId) {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();

        membreEntrepriseRepository
                .findByUtilisateurIdAndEntrepriseId(utilisateur.getId(), entrepriseId)
                .filter(membre -> membre.getStatut() == StatutInvitation.ACCEPTED)
                .orElseThrow(() -> new RuntimeException("Acces refuse"));

        return projetRepository
                .findByEntrepriseId(entrepriseId)
                .stream()
                .map(projet -> {
                    RoleProjet role = projectAccessHelper
                            .resolveRole(utilisateur, projet)
                            .orElse(null);

                    return toDto(projet, role);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<MemberResponseDto> getMembresProjet(Long projetId) {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));

        projectAccessHelper.requireAccess(utilisateur, projet);

        List<MemberResponseDto> members = new ArrayList<>();
        Set<Long> userIds = new HashSet<>();

        membreProjetRepository
                .findByProjetIdAndStatut(projetId, StatutInvitation.ACCEPTED)
                .stream()
                .forEach(membreProjet -> {
                    members.add(toMemberDto(membreProjet));
                    userIds.add(membreProjet.getUtilisateur().getId());
                });

        if (projet.getType() == TypeProjet.ENTREPRISE && projet.getEntreprise() != null) {
            membreEntrepriseRepository
                    .findByEntrepriseIdAndStatut(projet.getEntreprise().getId(), StatutInvitation.ACCEPTED)
                    .stream()
                    .filter(membreEntreprise -> membreEntreprise.getRole() == RoleEntreprise.ADMIN)
                    .filter(membreEntreprise -> !userIds.contains(membreEntreprise.getUtilisateur().getId()))
                    .map(this::toEntrepriseAdminMemberDto)
                    .forEach(members::add);
        }

        return members;
    }

    @Override
    @Transactional
    public void inviterMembreProjet(Long projetId, InviteMembreProjetRequestDto request) {

        Utilisateur inviter = authHelper.getUtilisateurCourant();

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));

        if (projet.getStatut() != StatutProjet.ACTIVE) {
            throw new RuntimeException("Impossible d'inviter des membres - le projet n'est pas actif");
        }

        projectAccessHelper.requireManager(inviter, projet);

        Utilisateur invitedUser = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec cet email"));

        if (projet.getType() == TypeProjet.ENTREPRISE) {
            membreEntrepriseRepository
                    .findByUtilisateurIdAndEntrepriseId(invitedUser.getId(), projet.getEntreprise().getId())
                    .filter(m -> m.getStatut() == StatutInvitation.ACCEPTED)
                    .orElseThrow(() -> new RuntimeException(
                            "Pour un projet d'entreprise, l'invité doit être membre accepté de l'entreprise"));
        }

        if (projectAccessHelper.resolveRole(invitedUser, projet).isPresent()) {
            throw new RuntimeException("Cet utilisateur est deja membre du projet");
        }

        MembreProjet membreProjet = membreProjetRepository
                .findByUtilisateurIdAndProjetId(invitedUser.getId(), projetId)
                .map(existingMembership -> {
                    if (existingMembership.getStatut() != StatutInvitation.REJECTED) {
                        throw new RuntimeException("Cet utilisateur est deja membre ou invite au projet");
                    }

                    existingMembership.setRole(RoleProjet.MEMBER);
                    existingMembership.setStatut(StatutInvitation.PENDING);
                    existingMembership.setInvitePar(inviter);
                    existingMembership.setDateAdhesion(LocalDateTime.now());

                    return existingMembership;
                })
                .orElseGet(() -> MembreProjet.builder()
                        .utilisateur(invitedUser)
                        .projet(projet)
                        .role(RoleProjet.MEMBER)
                        .statut(StatutInvitation.PENDING)
                        .invitePar(inviter)
                        .dateAdhesion(LocalDateTime.now())
                        .build());

        membreProjetRepository.save(membreProjet);

        emailService.sendProjectInvitationEmail(invitedUser.getEmail(), projet.getNom());
    }

    private void verifierAdminEntreprise(Utilisateur utilisateur, Long entrepriseId) {
        MembreEntreprise membership = membreEntrepriseRepository
                .findByUtilisateurIdAndEntrepriseId(utilisateur.getId(), entrepriseId)
                .orElseThrow(() -> new RuntimeException("Acces refuse - vous n'appartenez pas a cette entreprise"));

        if (membership.getStatut() != StatutInvitation.ACCEPTED || membership.getRole() != RoleEntreprise.ADMIN) {
            throw new RuntimeException("Seul l'ADMIN de l'entreprise peut effectuer cette action");
        }
    }

    private void notifierAdminsProjetEnAttente(Projet projet, Utilisateur createur) {

        membreEntrepriseRepository
                .findByEntrepriseIdAndStatut(projet.getEntreprise().getId(), StatutInvitation.ACCEPTED)
                .stream()
                .filter(membre -> membre.getRole() == RoleEntreprise.ADMIN)
                .map(MembreEntreprise::getUtilisateur)
                .filter(admin -> !admin.getId().equals(createur.getId()))
                .forEach(admin -> emailService.sendProjectValidationEmail(
                        admin.getEmail(),
                        projet.getNom(),
                        projet.getEntreprise().getNom()
                ));
    }

    private ProjetResponseDto toDto(Projet projet, RoleProjet role) {
        return ProjetResponseDto.builder()
                .id(projet.getId())
                .nom(projet.getNom())
                .description(projet.getDescription())
                .type(projet.getType().name())
                .statut(projet.getStatut().name())
                .entrepriseId(projet.getEntreprise() != null ? projet.getEntreprise().getId() : null)
                .entrepriseNom(projet.getEntreprise() != null ? projet.getEntreprise().getNom() : null)
                .createurId(projet.getCreateur().getId())
                .createurNom(projet.getCreateur().getPrenom() + " " + projet.getCreateur().getNom())
                .role(role != null ? role.name() : null)
                .dateDebut(projet.getDateDebut())
                .dateFin(projet.getDateFin())
                .dateCreation(projet.getDateCreation())
                .build();
    }

    private MemberResponseDto toMemberDto(MembreProjet membre) {

        Utilisateur utilisateur = membre.getUtilisateur();
        Utilisateur inviter = membre.getInvitePar();

        return MemberResponseDto.builder()
                .id(membre.getId())
                .userId(utilisateur.getId())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .email(utilisateur.getEmail())
                .role(membre.getRole().name())
                .status(membre.getStatut().name())
                .invitedById(inviter != null ? inviter.getId() : null)
                .invitedByName(inviter != null ? inviter.getPrenom() + " " + inviter.getNom() : null)
                .invitedAt(membre.getDateAdhesion())
                .joinedAt(membre.getDateAdhesion())
                .build();
    }

    private MemberResponseDto toEntrepriseAdminMemberDto(MembreEntreprise membre) {

        Utilisateur utilisateur = membre.getUtilisateur();
        Utilisateur inviter = membre.getInvitePar();

        return MemberResponseDto.builder()
                .id(-membre.getId())
                .userId(utilisateur.getId())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .email(utilisateur.getEmail())
                .role(RoleProjet.MANAGER.name())
                .status(StatutInvitation.ACCEPTED.name())
                .invitedById(inviter != null ? inviter.getId() : null)
                .invitedByName(inviter != null ? inviter.getPrenom() + " " + inviter.getNom() : null)
                .invitedAt(membre.getDateInvitation())
                .joinedAt(membre.getDateReponse() != null ? membre.getDateReponse() : membre.getDateInvitation())
                .build();
    }
}
