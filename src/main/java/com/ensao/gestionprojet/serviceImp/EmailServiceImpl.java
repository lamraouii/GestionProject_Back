package com.ensao.gestionprojet.serviceImp;

import com.ensao.gestionprojet.service.EmailService;

import lombok.RequiredArgsConstructor;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void envoyerEmailConfirmation(
            String to,
            String confirmationLink
    ) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(to);

        message.setSubject(
                "Confirmation de votre compte"
        );

        message.setText(
                "Cliquez sur ce lien pour confirmer votre compte : \n"
                        + confirmationLink
        );

        mailSender.send(message);
    }

    @Override
    @Async
    public void sendInvitationEmail(String to, String entrepriseName) {

        String subject = "Invitation à rejoindre " + entrepriseName;

        String body = "Vous avez été invité à rejoindre l'entreprise: "
                + entrepriseName
                + ". Connectez-vous pour accepter l'invitation.";

        SimpleMailMessage msg =new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        try {
            mailSender.send(msg);
        } catch (MailException exception) {
            System.err.println("Impossible d'envoyer l'email d'invitation entreprise: " + exception.getMessage());
        }

    }

    @Override
    @Async
    public void sendProjectInvitationEmail(String to, String projectName) {

        String subject = "Invitation a rejoindre le projet " + projectName;

        String body = "Vous avez ete invite a rejoindre le projet: "
                + projectName
                + ". Connectez-vous pour accepter l'invitation.";

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        try {
            mailSender.send(msg);
        } catch (MailException exception) {
            System.err.println("Impossible d'envoyer l'email d'invitation projet: " + exception.getMessage());
        }
    }

    @Override
    @Async
    public void sendProjectValidationEmail(String to, String projectName, String entrepriseName) {

        String subject = "Projet en attente de validation - " + entrepriseName;

        String body = "Le projet \"" + projectName + "\" a ete cree dans l'entreprise "
                + entrepriseName
                + ". Connectez-vous pour valider ou rejeter cette demande.";

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);

        try {
            mailSender.send(msg);
        } catch (MailException exception) {
            System.err.println("Impossible d'envoyer l'email de validation projet: " + exception.getMessage());
        }
    }

}
