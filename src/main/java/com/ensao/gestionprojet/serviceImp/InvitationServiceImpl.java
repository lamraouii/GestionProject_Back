package com.ensao.gestionprojet.serviceImp;

import com.ensao.gestionprojet.dto.InviteMemberRequestDto;
import com.ensao.gestionprojet.entity.Entreprise;
import com.ensao.gestionprojet.entity.MembreEntreprise;
import com.ensao.gestionprojet.entity.Utilisateur;
import com.ensao.gestionprojet.enums.RoleEntreprise;
import com.ensao.gestionprojet.enums.StatutInvitation;
import com.ensao.gestionprojet.repository.EntrepriseRepository;
import com.ensao.gestionprojet.repository.MembreEntrepriseRepository;
import com.ensao.gestionprojet.repository.UtilisateurRepo;
import com.ensao.gestionprojet.service.EmailService;
import com.ensao.gestionprojet.service.InvitationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private final EntrepriseRepository entrepriseRepository;
    private final UtilisateurRepo utilisateurRepository;
    private final MembreEntrepriseRepository membreEntrepriseRepository;
    private final EmailService emailService;

    public void inviterMembre(Long entrepriseId, InviteMemberRequestDto request) {

        // 1. current user
        String emailAdmin = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Utilisateur admin = utilisateurRepository.findByEmail(emailAdmin)
                .orElseThrow(() -> new RuntimeException("Admin introuvable"));

        // 2. entreprise
        Entreprise entreprise = entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));

        // 3. check ADMIN rights
        MembreEntreprise adminMembership = membreEntrepriseRepository
                .findByUtilisateurIdAndEntrepriseId(admin.getId(), entrepriseId)
                .orElseThrow(() -> new RuntimeException("Accès refusé"));

        if (!adminMembership.getRole().equals(RoleEntreprise.ADMIN)) {
            throw new RuntimeException("Seuls les ADMIN peuvent inviter");
        }

        // 4. find invited user
        Utilisateur invitedUser = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur invité introuvable"));

        // 5. prevent duplicate invitation
        if (membreEntrepriseRepository
                .findByUtilisateurIdAndEntrepriseId(invitedUser.getId(), entrepriseId)
                .isPresent()) {
            throw new RuntimeException("Utilisateur déjà invité ou membre");
        }

        // 6. create membership
        MembreEntreprise membre = new MembreEntreprise();
        membre.setUtilisateur(invitedUser);
        membre.setEntreprise(entreprise);

        membre.setRole(RoleEntreprise.MEMBER);
        membre.setStatut(StatutInvitation.PENDING);

        membre.setInvitePar(admin);
        membre.setDateInvitation(LocalDateTime.now());

        membreEntrepriseRepository.save(membre);

        // 7. send email
        emailService.sendInvitationEmail(
                invitedUser.getEmail(),
                entreprise.getNom()
        );

}

    @Override
    @Transactional
    public void accepterInvitation(Long invitationId) {

        // 1. authenticated user
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // 2. invitation
        MembreEntreprise invitation = membreEntrepriseRepository
                .findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation introuvable"));

        // 3. verify ownership
        if (!invitation.getUtilisateur().getId().equals(utilisateur.getId())) {
            throw new RuntimeException("Accès refusé");
        }

        // 4. verify status
        if (!invitation.getStatut().equals(StatutInvitation.PENDING)) {
            throw new RuntimeException("Invitation déjà traitée");
        }

        // 5. accept invitation
        invitation.setStatut(StatutInvitation.ACCEPTED);

        invitation.setDateReponse(LocalDateTime.now());

        membreEntrepriseRepository.save(invitation);
    }

    @Override
    @Transactional
    public void refuserInvitation(Long invitationId) {

        // 1. authenticated user
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // 2. invitation
        MembreEntreprise invitation = membreEntrepriseRepository
                .findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation introuvable"));

        // 3. verify ownership
        if (!invitation.getUtilisateur().getId().equals(utilisateur.getId())) {
            throw new RuntimeException("Accès refusé");
        }

        // 4. verify status
        if (!invitation.getStatut().equals(StatutInvitation.PENDING)) {
            throw new RuntimeException("Invitation déjà traitée");
        }

        // 5. reject invitation
        invitation.setStatut(StatutInvitation.REJECTED);

        invitation.setDateReponse(LocalDateTime.now());

        membreEntrepriseRepository.save(invitation);
    }
}
