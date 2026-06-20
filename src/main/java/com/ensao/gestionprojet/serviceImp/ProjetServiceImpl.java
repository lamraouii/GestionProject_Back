package com.ensao.gestionprojet.serviceImp;

import com.ensao.gestionprojet.dto.CreateProjetRequestDto;
import com.ensao.gestionprojet.dto.InviteMembreProjetRequestDto;
import com.ensao.gestionprojet.dto.MemberResponseDto;
import com.ensao.gestionprojet.dto.ProjetResponseDto;
import com.ensao.gestionprojet.entity.*;
import com.ensao.gestionprojet.enums.*;
import com.ensao.gestionprojet.helpers.AuthHelper;
import com.ensao.gestionprojet.repository.*;
import com.ensao.gestionprojet.service.EmailService;
import com.ensao.gestionprojet.service.ProjetService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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
    private final EmailService emailService;

    // Créer un projet personnel ou d'entreprise
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
            //  Projet personnel : actif immédiatement, pas d'entreprise
            projet.setEntreprise(null);
            projet.setStatut(StatutProjet.ACTIVE);

        } else {
            // Projet d'entreprise : PENDING jusqu'à validation ADMIN
            if (request.getEntrepriseId() == null) {
                throw new RuntimeException("L'identifiant de l'entreprise est obligatoire pour un projet d'entreprise");
            }

            Entreprise entreprise = entrepriseRepository.findById(request.getEntrepriseId())
                    .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));

            // Vérifier que le créateur est membre ACCEPTED de l'entreprise
            membreEntrepriseCreateur = membreEntrepriseRepository
                    .findByUtilisateurIdAndEntrepriseId(createur.getId(), entreprise.getId())
                    .filter(m -> m.getStatut() == StatutInvitation.ACCEPTED)
                    .orElseThrow(() -> new RuntimeException(
                            "Vous devez être membre accepté de l'entreprise pour créer un projet d'entreprise"));

            projet.setEntreprise(entreprise);
            projet.setStatut(
                    membreEntrepriseCreateur.getRole() == RoleEntreprise.ADMIN
                            ? StatutProjet.ACTIVE
                            : StatutProjet.PENDING
            );
        }

        Projet savedProjet = projetRepository.save(projet);

        // Le créateur devient automatiquement MANAGER du projet
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

    // Valider un projet d'entreprise (ADMIN)
    @Override
    @Transactional
    public ProjetResponseDto validerProjet(Long projetId) {

        Utilisateur admin = authHelper.getUtilisateurCourant();

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));

        // Vérifier que c'est un projet d'entreprise en PENDING
        if (projet.getType() != TypeProjet.ENTREPRISE) {
            throw new RuntimeException("Seuls les projets d'entreprise nécessitent une validation");
        }
        if (projet.getStatut() != StatutProjet.PENDING) {
            throw new RuntimeException("Ce projet n'est pas en attente de validation");
        }

        // Vérifier que l'utilisateur est ADMIN de l'entreprise
        verifierAdminEntreprise(admin, projet.getEntreprise().getId());

        projet.setStatut(StatutProjet.ACTIVE);
        Projet savedProjet = projetRepository.save(projet);

        return toDto(savedProjet, null);
    }

    // Rejeter un projet d'entreprise (ADMIN)
    @Override
    @Transactional
    public ProjetResponseDto rejeterProjet(Long projetId) {

        Utilisateur admin = authHelper.getUtilisateurCourant();

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));

        if (projet.getType() != TypeProjet.ENTREPRISE) {
            throw new RuntimeException("Seuls les projets d'entreprise nécessitent une validation");
        }
        if (projet.getStatut() != StatutProjet.PENDING) {
            throw new RuntimeException("Ce projet n'est pas en attente de validation");
        }

        verifierAdminEntreprise(admin, projet.getEntreprise().getId());

        projet.setStatut(StatutProjet.REJECTED);
        Projet savedProjet = projetRepository.save(projet);

        return toDto(savedProjet, null);
    }

    // Récupérer les détails d'un projet
    @Override
    public ProjetResponseDto getProjet(Long projetId) {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));

        // Vérifier que l'utilisateur est membre du projet
        MembreProjet membership = membreProjetRepository
                .findByUtilisateurIdAndProjetIdAndStatut(
                        utilisateur.getId(), projetId, StatutInvitation.ACCEPTED)
                .orElseThrow(() -> new RuntimeException("Accès refusé — vous n'êtes pas membre de ce projet"));

        return toDto(projet, membership.getRole());
    }

    // Récupérer tous les projets de l'utilisateur courant
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

        MembreEntreprise membreEntreprise = membreEntrepriseRepository
                .findByUtilisateurIdAndEntrepriseId(
                        utilisateur.getId(),
                        entrepriseId
                )
                .filter(membre -> membre.getStatut() == StatutInvitation.ACCEPTED)
                .orElseThrow(() -> new RuntimeException("Acces refuse"));

        return projetRepository
                .findByEntrepriseId(entrepriseId)
                .stream()
                .map(projet -> {
                    RoleProjet role = membreProjetRepository
                            .findByUtilisateurIdAndProjetIdAndStatut(
                                    utilisateur.getId(),
                                    projet.getId(),
                                    StatutInvitation.ACCEPTED
                            )
                            .map(MembreProjet::getRole)
                            .orElse(null);

                    if (role == null && membreEntreprise.getRole() == RoleEntreprise.ADMIN) {
                        return toDto(projet, RoleProjet.MANAGER);
                    }

                    return toDto(projet, role);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<MemberResponseDto> getMembresProjet(Long projetId) {

        Utilisateur utilisateur = authHelper.getUtilisateurCourant();

        membreProjetRepository
                .findByUtilisateurIdAndProjetIdAndStatut(
                        utilisateur.getId(),
                        projetId,
                        StatutInvitation.ACCEPTED
                )
                .orElseThrow(() -> new RuntimeException("Acces refuse"));

        return membreProjetRepository
                .findByProjetIdAndStatut(
                        projetId,
                        StatutInvitation.ACCEPTED
                )
                .stream()
                .map(this::toMemberDto)
                .collect(Collectors.toList());
    }

    // Inviter un membre au projet (MANAGER)
    @Override
    @Transactional
    public void inviterMembreProjet(Long projetId, InviteMembreProjetRequestDto request) {

        Utilisateur manager = authHelper.getUtilisateurCourant();

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));

        // Vérifier que le projet est ACTIVE
        if (projet.getStatut() != StatutProjet.ACTIVE) {
            throw new RuntimeException("Impossible d'inviter des membres — le projet n'est pas actif");
        }

        // Vérifier que l'utilisateur courant est MANAGER du projet
        membreProjetRepository
                .findByUtilisateurIdAndProjetIdAndRole(manager.getId(), projetId, RoleProjet.MANAGER)
                .orElseThrow(() -> new RuntimeException("Seul le Manager peut inviter des membres"));

        // Trouver l'utilisateur à inviter
        Utilisateur invitedUser = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec cet email"));

        // Vérifier qu'il n'est pas déjà membre
        if (membreProjetRepository.findByUtilisateurIdAndProjetId(invitedUser.getId(), projetId).isPresent()) {
            throw new RuntimeException("Cet utilisateur est déjà membre ou invité au projet");
        }

        // Pour un projet ENTREPRISE : l'invité doit être membre ACCEPTED de l'entreprise
        if (projet.getType() == TypeProjet.ENTREPRISE) {
            membreEntrepriseRepository
                    .findByUtilisateurIdAndEntrepriseId(invitedUser.getId(), projet.getEntreprise().getId())
                    .filter(m -> m.getStatut() == StatutInvitation.ACCEPTED)
                    .orElseThrow(() -> new RuntimeException(
                            "Pour un projet d'entreprise, l'invité doit être membre accepté de l'entreprise"));
        }

        // Créer le membership avec statut ACCEPTED (ajout direct par le MANAGER)
        MembreProjet membreProjet = MembreProjet.builder()
                .utilisateur(invitedUser)
                .projet(projet)
                .role(RoleProjet.MEMBER)
                .statut(StatutInvitation.PENDING)
                .invitePar(manager)
                .dateAdhesion(LocalDateTime.now())
                .build();

        membreProjetRepository.save(membreProjet);

        emailService.sendProjectInvitationEmail(
                invitedUser.getEmail(),
                projet.getNom()
        );
    }

    // Helpers privés

    private void verifierAdminEntreprise(Utilisateur utilisateur, Long entrepriseId) {
        MembreEntreprise membership = membreEntrepriseRepository
                .findByUtilisateurIdAndEntrepriseId(utilisateur.getId(), entrepriseId)
                .orElseThrow(() -> new RuntimeException("Accès refusé — vous n'appartenez pas à cette entreprise"));

        if (membership.getRole() != RoleEntreprise.ADMIN) {
            throw new RuntimeException("Seul l'ADMIN de l'entreprise peut effectuer cette action");
        }
    }

    private void notifierAdminsProjetEnAttente(Projet projet, Utilisateur createur) {

        membreEntrepriseRepository
                .findByEntrepriseIdAndStatut(
                        projet.getEntreprise().getId(),
                        StatutInvitation.ACCEPTED
                )
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
}
