package com.ensao.gestionprojet.serviceImp;

import com.ensao.gestionprojet.dto.CreateEntrepriseRequestDto;
import com.ensao.gestionprojet.dto.EntrepriseResponseDto;
import com.ensao.gestionprojet.entity.Entreprise;
import com.ensao.gestionprojet.entity.MembreEntreprise;
import com.ensao.gestionprojet.entity.MembreProjet;
import com.ensao.gestionprojet.entity.Utilisateur;
import com.ensao.gestionprojet.enums.RoleEntreprise;
import com.ensao.gestionprojet.enums.StatutInvitation;
import com.ensao.gestionprojet.repository.EntrepriseRepository;
import com.ensao.gestionprojet.repository.MembreEntrepriseRepository;
import com.ensao.gestionprojet.repository.MembreProjetRepository;
import com.ensao.gestionprojet.repository.UtilisateurRepo;
import com.ensao.gestionprojet.service.EntrepriseService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EntrepriseServiceImpl implements EntrepriseService {


    private final EntrepriseRepository entrepriseRepository;
    private final MembreEntrepriseRepository membreEntrepriseRepository;
    private final UtilisateurRepo utilisateurRepository;

    private final MembreProjetRepository membreProjetRepository;

    @Override
    @Transactional  // bach ytsavaw bjuj fd9a both or nothing
    public EntrepriseResponseDto creerEntreprise(CreateEntrepriseRequestDto request) {

        // 1. récupérer utilisateur authentifié
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        String email = authentication.getName();

        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // 2. créer entreprise
        Entreprise entreprise = new Entreprise();

        entreprise.setNom(request.getNom());
        entreprise.setDescription(request.getDescription());
        entreprise.setUrlLogo(request.getUrlLogo());

        entreprise.setCreateur(utilisateur);

        Entreprise savedEntreprise = entrepriseRepository.save(entreprise);

        // 3. créer membre entreprise
        MembreEntreprise membreEntreprise = new MembreEntreprise();

        membreEntreprise.setUtilisateur(utilisateur);
        membreEntreprise.setEntreprise(savedEntreprise);

        membreEntreprise.setRole(RoleEntreprise.ADMIN);

        membreEntreprise.setStatut(StatutInvitation.ACCEPTED);

        membreEntreprise.setInvitePar(utilisateur);

        membreEntreprise.setDateInvitation(LocalDateTime.now());

        membreEntreprise.setDateReponse(LocalDateTime.now());

        membreEntrepriseRepository.save(membreEntreprise);

        // 4. retourner réponse
        return EntrepriseResponseDto.builder()
                .id(savedEntreprise.getId())
                .nom(savedEntreprise.getNom())
                .description(savedEntreprise.getDescription())
                .urlLogo(savedEntreprise.getUrlLogo())
                .role(RoleEntreprise.ADMIN.name())
                .build();
    }


    @Override
    @Transactional
    public void retirerMembre(
            Long entrepriseId,
            Long utilisateurId
    ) {

        String emailAdmin = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Utilisateur admin = utilisateurRepository
                .findByEmail(emailAdmin)
                .orElseThrow(() ->
                        new RuntimeException("Admin introuvable"));

        MembreEntreprise adminMembership =
                membreEntrepriseRepository
                        .findByUtilisateurIdAndEntrepriseId(
                                admin.getId(),
                                entrepriseId
                        )
                        .orElseThrow(() ->
                                new RuntimeException("Accès refusé"));

        if (adminMembership.getRole() != RoleEntreprise.ADMIN) {
            throw new RuntimeException(
                    "Seul un ADMIN peut retirer un membre"
            );
        }

        if (admin.getId().equals(utilisateurId)) {
            throw new RuntimeException(
                    "Un ADMIN ne peut pas se retirer lui-même"
            );
        }

        MembreEntreprise membre =
                membreEntrepriseRepository
                        .findByUtilisateurIdAndEntrepriseId(
                                utilisateurId,
                                entrepriseId
                        )
                        .orElseThrow(() ->
                                new RuntimeException("Membre introuvable"));

        List<MembreProjet> membershipsProjet =
                membreProjetRepository
                        .findByUtilisateurIdAndProjetEntrepriseId(
                                utilisateurId,
                                entrepriseId
                        );

        membreProjetRepository.deleteAll(
                membershipsProjet
        );

        membreEntrepriseRepository.delete(
                membre
        );
    }


}
