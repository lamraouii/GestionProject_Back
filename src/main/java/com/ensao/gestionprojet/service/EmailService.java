package com.ensao.gestionprojet.service;

public interface EmailService {

    void envoyerEmailConfirmation(
            String to,
            String confirmationLink
    );
}
