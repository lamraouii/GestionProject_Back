package com.ensao.gestionprojet.service;

public interface EmailService {

    void envoyerEmailConfirmation(
            String to,
            String confirmationLink
    );
    void sendInvitationEmail(String to, String entrepriseName);

    void sendProjectInvitationEmail(String to, String projectName);

    void sendProjectValidationEmail(String to, String projectName, String entrepriseName);
}
