package com.ensao.gestionprojet.serviceImp;

import com.ensao.gestionprojet.dto.InviteMemberRequestDto;
import com.ensao.gestionprojet.dto.InvitationResponseDto;
import com.ensao.gestionprojet.entity.Entreprise;
import com.ensao.gestionprojet.entity.MembreEntreprise;
import com.ensao.gestionprojet.entity.MembreProjet;
import com.ensao.gestionprojet.entity.Projet;
import com.ensao.gestionprojet.entity.Utilisateur;
import com.ensao.gestionprojet.enums.RoleEntreprise;
import com.ensao.gestionprojet.enums.StatutProjet;
import com.ensao.gestionprojet.enums.StatutInvitation;
import com.ensao.gestionprojet.repository.EntrepriseRepository;
import com.ensao.gestionprojet.repository.MembreEntrepriseRepository;
import com.ensao.gestionprojet.repository.MembreProjetRepository;
import com.ensao.gestionprojet.repository.ProjetRepository;
import com.ensao.gestionprojet.repository.UtilisateurRepo;
import com.ensao.gestionprojet.service.EmailService;
import com.ensao.gestionprojet.service.InvitationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private final EntrepriseRepository entrepriseRepository;
    private final UtilisateurRepo utilisateurRepository;
    private final MembreEntrepriseRepository membreEntrepriseRepository;
    private final MembreProjetRepository membreProjetRepository;
    private final ProjetRepository projetRepository;
    private final EmailService emailService;

    @Override
    public void inviterMembre(Long entrepriseId, InviteMemberRequestDto request) {

        Utilisateur admin = getUtilisateurCourant();

        Entreprise entreprise = entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));

        MembreEntreprise adminMembership = membreEntrepriseRepository
                .findByUtilisateurIdAndEntrepriseId(admin.getId(), entrepriseId)
                .filter(membre -> membre.getStatut() == StatutInvitation.ACCEPTED)
                .orElseThrow(() -> new RuntimeException("Acces refuse"));

        if (!adminMembership.getRole().equals(RoleEntreprise.ADMIN)) {
            throw new RuntimeException("Seuls les ADMIN peuvent inviter");
        }

        Utilisateur invitedUser = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur invite introuvable"));

        MembreEntreprise membre = membreEntrepriseRepository
                .findByUtilisateurIdAndEntrepriseId(invitedUser.getId(), entrepriseId)
                .map(existingMembership -> {
                    if (existingMembership.getStatut() != StatutInvitation.REJECTED) {
                        throw new RuntimeException("Utilisateur deja invite ou membre");
                    }

                    existingMembership.setRole(RoleEntreprise.MEMBER);
                    existingMembership.setStatut(StatutInvitation.PENDING);
                    existingMembership.setInvitePar(admin);
                    existingMembership.setDateInvitation(LocalDateTime.now());
                    existingMembership.setDateReponse(null);

                    return existingMembership;
                })
                .orElseGet(() -> {
                    MembreEntreprise newMembership = new MembreEntreprise();
                    newMembership.setUtilisateur(invitedUser);
                    newMembership.setEntreprise(entreprise);
                    newMembership.setRole(RoleEntreprise.MEMBER);
                    newMembership.setStatut(StatutInvitation.PENDING);
                    newMembership.setInvitePar(admin);
                    newMembership.setDateInvitation(LocalDateTime.now());
                    return newMembership;
                });

        membreEntrepriseRepository.save(membre);

        emailService.sendInvitationEmail(
                invitedUser.getEmail(),
                entreprise.getNom()
        );
    }

    @Override
    public List<InvitationResponseDto> getMesInvitations() {

        Utilisateur utilisateur = getUtilisateurCourant();
        List<InvitationResponseDto> invitations = new ArrayList<>();

        membreEntrepriseRepository
                .findByUtilisateurIdAndStatut(
                        utilisateur.getId(),
                        StatutInvitation.PENDING
                )
                .stream()
                .map(this::toCompanyInvitationDto)
                .forEach(invitations::add);

        membreProjetRepository
                .findByUtilisateurIdAndStatut(
                        utilisateur.getId(),
                        StatutInvitation.PENDING
                )
                .stream()
                .map(this::toProjectInvitationDto)
                .forEach(invitations::add);

        membreEntrepriseRepository
                .findByUtilisateurIdAndStatut(
                        utilisateur.getId(),
                        StatutInvitation.ACCEPTED
                )
                .stream()
                .filter(membre -> membre.getRole() == RoleEntreprise.ADMIN)
                .flatMap(membre -> projetRepository
                        .findByEntrepriseIdAndStatut(
                                membre.getEntreprise().getId(),
                                StatutProjet.PENDING
                        )
                        .stream())
                .map(this::toProjectValidationInvitationDto)
                .forEach(invitations::add);

        return invitations;
    }

    @Override
    @Transactional
    public void accepterInvitation(Long invitationId) {
        accepterInvitationEntreprise(invitationId);
    }

    @Override
    @Transactional
    public void refuserInvitation(Long invitationId) {
        refuserInvitationEntreprise(invitationId);
    }

    @Override
    @Transactional
    public void accepterInvitation(String invitationKey) {

        InvitationKey key = parseInvitationKey(invitationKey);

        if ("company".equals(key.type)) {
            accepterInvitationEntreprise(key.id);
            return;
        }

        if ("project".equals(key.type)) {
            accepterInvitationProjet(key.id);
            return;
        }

        if ("validation".equals(key.type)) {
            accepterValidationProjet(key.id);
            return;
        }

        throw new RuntimeException("Type d'invitation invalide");
    }

    @Override
    @Transactional
    public void refuserInvitation(String invitationKey) {

        InvitationKey key = parseInvitationKey(invitationKey);

        if ("company".equals(key.type)) {
            refuserInvitationEntreprise(key.id);
            return;
        }

        if ("project".equals(key.type)) {
            refuserInvitationProjet(key.id);
            return;
        }

        if ("validation".equals(key.type)) {
            refuserValidationProjet(key.id);
            return;
        }

        throw new RuntimeException("Type d'invitation invalide");
    }

    private void accepterInvitationEntreprise(Long invitationId) {

        MembreEntreprise invitation = getOwnedCompanyInvitation(invitationId);

        if (!invitation.getStatut().equals(StatutInvitation.PENDING)) {
            throw new RuntimeException("Invitation deja traitee");
        }

        invitation.setStatut(StatutInvitation.ACCEPTED);
        invitation.setDateReponse(LocalDateTime.now());

        membreEntrepriseRepository.save(invitation);
    }

    private void refuserInvitationEntreprise(Long invitationId) {

        MembreEntreprise invitation = getOwnedCompanyInvitation(invitationId);

        if (!invitation.getStatut().equals(StatutInvitation.PENDING)) {
            throw new RuntimeException("Invitation deja traitee");
        }

        invitation.setStatut(StatutInvitation.REJECTED);
        invitation.setDateReponse(LocalDateTime.now());

        membreEntrepriseRepository.save(invitation);
    }

    private void accepterInvitationProjet(Long invitationId) {

        MembreProjet invitation = getOwnedProjectInvitation(invitationId);

        if (!invitation.getStatut().equals(StatutInvitation.PENDING)) {
            throw new RuntimeException("Invitation deja traitee");
        }

        invitation.setStatut(StatutInvitation.ACCEPTED);
        invitation.setDateAdhesion(LocalDateTime.now());

        membreProjetRepository.save(invitation);
    }

    private void refuserInvitationProjet(Long invitationId) {

        MembreProjet invitation = getOwnedProjectInvitation(invitationId);

        if (!invitation.getStatut().equals(StatutInvitation.PENDING)) {
            throw new RuntimeException("Invitation deja traitee");
        }

        invitation.setStatut(StatutInvitation.REJECTED);
        invitation.setDateAdhesion(LocalDateTime.now());

        membreProjetRepository.save(invitation);
    }

    private void accepterValidationProjet(Long projetId) {

        Projet projet = getProjetAValider(projetId);
        projet.setStatut(StatutProjet.ACTIVE);
        projetRepository.save(projet);
    }

    private void refuserValidationProjet(Long projetId) {

        Projet projet = getProjetAValider(projetId);
        projet.setStatut(StatutProjet.REJECTED);
        projetRepository.save(projet);
    }

    private MembreEntreprise getOwnedCompanyInvitation(Long invitationId) {

        Utilisateur utilisateur = getUtilisateurCourant();

        MembreEntreprise invitation = membreEntrepriseRepository
                .findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation introuvable"));

        if (!invitation.getUtilisateur().getId().equals(utilisateur.getId())) {
            throw new RuntimeException("Acces refuse");
        }

        return invitation;
    }

    private MembreProjet getOwnedProjectInvitation(Long invitationId) {

        Utilisateur utilisateur = getUtilisateurCourant();

        MembreProjet invitation = membreProjetRepository
                .findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation introuvable"));

        if (!invitation.getUtilisateur().getId().equals(utilisateur.getId())) {
            throw new RuntimeException("Acces refuse");
        }

        return invitation;
    }

    private Projet getProjetAValider(Long projetId) {

        Utilisateur utilisateur = getUtilisateurCourant();

        Projet projet = projetRepository
                .findById(projetId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable"));

        if (projet.getEntreprise() == null) {
            throw new RuntimeException("Ce projet n'appartient pas a une entreprise");
        }

        membreEntrepriseRepository
                .findByUtilisateurIdAndEntrepriseId(
                        utilisateur.getId(),
                        projet.getEntreprise().getId()
                )
                .filter(membre -> membre.getStatut() == StatutInvitation.ACCEPTED)
                .filter(membre -> membre.getRole() == RoleEntreprise.ADMIN)
                .orElseThrow(() -> new RuntimeException("Acces refuse"));

        if (projet.getStatut() != StatutProjet.PENDING) {
            throw new RuntimeException("Ce projet n'est pas en attente de validation");
        }

        return projet;
    }

    private InvitationResponseDto toCompanyInvitationDto(MembreEntreprise invitation) {

        Entreprise entreprise = invitation.getEntreprise();
        Utilisateur inviter = invitation.getInvitePar();

        return InvitationResponseDto.builder()
                .id("company-" + invitation.getId())
                .invitationId(invitation.getId())
                .type("COMPANY")
                .title(entreprise.getNom())
                .description("Invitation a rejoindre l'entreprise " + entreprise.getNom())
                .role(invitation.getRole().name())
                .status(invitation.getStatut().name())
                .invitedById(inviter != null ? inviter.getId() : null)
                .invitedByName(inviter != null ? inviter.getPrenom() + " " + inviter.getNom() : null)
                .createdAt(invitation.getDateInvitation())
                .build();
    }

    private InvitationResponseDto toProjectInvitationDto(MembreProjet invitation) {

        Projet projet = invitation.getProjet();
        Utilisateur inviter = invitation.getInvitePar();

        return InvitationResponseDto.builder()
                .id("project-" + invitation.getId())
                .invitationId(invitation.getId())
                .type("PROJECT")
                .title(projet.getNom())
                .description("Invitation a rejoindre le projet " + projet.getNom())
                .role(invitation.getRole().name())
                .status(invitation.getStatut().name())
                .invitedById(inviter != null ? inviter.getId() : null)
                .invitedByName(inviter != null ? inviter.getPrenom() + " " + inviter.getNom() : null)
                .createdAt(invitation.getDateAdhesion())
                .build();
    }

    private InvitationResponseDto toProjectValidationInvitationDto(Projet projet) {

        Utilisateur createur = projet.getCreateur();

        return InvitationResponseDto.builder()
                .id("validation-" + projet.getId())
                .invitationId(projet.getId())
                .type("PROJECT_VALIDATION")
                .title(projet.getNom())
                .description("Demande de validation du projet " + projet.getNom()
                        + " dans l'entreprise " + projet.getEntreprise().getNom())
                .role("ADMIN")
                .status(projet.getStatut().name())
                .invitedById(createur != null ? createur.getId() : null)
                .invitedByName(createur != null ? createur.getPrenom() + " " + createur.getNom() : null)
                .createdAt(projet.getDateCreation())
                .build();
    }

    private Utilisateur getUtilisateurCourant() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return utilisateurRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

    private InvitationKey parseInvitationKey(String invitationKey) {

        String[] parts = invitationKey.split("-", 2);

        if (parts.length != 2) {
            throw new RuntimeException("Identifiant d'invitation invalide");
        }

        try {
            return new InvitationKey(parts[0].toLowerCase(), Long.parseLong(parts[1]));
        } catch (NumberFormatException exception) {
            throw new RuntimeException("Identifiant d'invitation invalide");
        }
    }

    private static class InvitationKey {
        private final String type;
        private final Long id;

        private InvitationKey(String type, Long id) {
            this.type = type;
            this.id = id;
        }
    }
}
