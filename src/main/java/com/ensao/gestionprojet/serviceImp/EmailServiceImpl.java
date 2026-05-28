package com.ensao.gestionprojet.serviceImp;

import com.ensao.gestionprojet.service.EmailService;

import lombok.RequiredArgsConstructor;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
    public void sendInvitationEmail(String to, String entrepriseName) {

        String subject = "Invitation à rejoindre " + entrepriseName;

        String body = "Vous avez été invité à rejoindre l'entreprise: "
                + entrepriseName
                + ". Connectez-vous pour accepter l'invitation.";

        SimpleMailMessage msg =new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        System.out.println("Before email");
        mailSender.send(msg);
        System.out.println("After email");

    }

}