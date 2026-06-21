package com.ensao.gestionprojet.serviceImp;

import com.ensao.gestionprojet.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String BREVO_SEND_EMAIL_URL = "https://api.brevo.com/v3/smtp/email";

    private final JavaMailSender mailSender;

    @Value("${brevo.api-key:}")
    private String brevoApiKey;

    @Value("${brevo.sender-email:}")
    private String brevoSenderEmail;

    @Value("${brevo.sender-name:TrackFlow}")
    private String brevoSenderName;

    @Override
    @Async
    public void envoyerEmailConfirmation(String to, String confirmationLink) {

        String subject = "Confirmation de votre compte";
        String body = "Cliquez sur ce lien pour confirmer votre compte : \n"
                + confirmationLink;

        sendEmail(to, subject, body, "email de confirmation");
    }

    @Override
    @Async
    public void sendInvitationEmail(String to, String entrepriseName) {

        String subject = "Invitation a rejoindre " + entrepriseName;
        String body = "Vous avez ete invite a rejoindre l'entreprise: "
                + entrepriseName
                + ". Connectez-vous pour accepter l'invitation.";

        sendEmail(to, subject, body, "email d'invitation entreprise");
    }

    @Override
    @Async
    public void sendProjectInvitationEmail(String to, String projectName) {

        String subject = "Invitation a rejoindre le projet " + projectName;
        String body = "Vous avez ete invite a rejoindre le projet: "
                + projectName
                + ". Connectez-vous pour accepter l'invitation.";

        sendEmail(to, subject, body, "email d'invitation projet");
    }

    @Override
    @Async
    public void sendProjectValidationEmail(String to, String projectName, String entrepriseName) {

        String subject = "Projet en attente de validation - " + entrepriseName;
        String body = "Le projet \"" + projectName + "\" a ete cree dans l'entreprise "
                + entrepriseName
                + ". Connectez-vous pour valider ou rejeter cette demande.";

        sendEmail(to, subject, body, "email de validation projet");
    }

    private void sendEmail(String to, String subject, String body, String context) {

        if (hasText(brevoApiKey)) {
            sendWithBrevoApi(to, subject, body, context);
            return;
        }

        sendWithSmtp(to, subject, body, context);
    }

    private void sendWithBrevoApi(String to, String subject, String body, String context) {

        if (!hasText(brevoSenderEmail)) {
            System.err.println("Impossible d'envoyer l'" + context + ": BREVO_SENDER_EMAIL est manquant");
            return;
        }

        try {
            String jsonBody = buildBrevoPayload(to, subject, body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_SEND_EMAIL_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("accept", "application/json")
                    .header("api-key", brevoApiKey)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                System.err.println("Impossible d'envoyer l'" + context + " via Brevo API: "
                        + response.statusCode() + " - " + response.body());
            }
        } catch (IOException exception) {
            System.err.println("Impossible d'envoyer l'" + context + " via Brevo API: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Envoi de l'" + context + " interrompu: " + exception.getMessage());
        }
    }

    private void sendWithSmtp(String to, String subject, String body, String context) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            System.err.println("Impossible d'envoyer l'" + context + ": " + exception.getMessage());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String buildBrevoPayload(String to, String subject, String body) {
        return "{"
                + "\"sender\":{\"name\":\"" + escapeJson(brevoSenderName) + "\",\"email\":\"" + escapeJson(brevoSenderEmail) + "\"},"
                + "\"to\":[{\"email\":\"" + escapeJson(to) + "\"}],"
                + "\"subject\":\"" + escapeJson(subject) + "\","
                + "\"textContent\":\"" + escapeJson(body) + "\""
                + "}";
    }

    private String escapeJson(String value) {
        if (value == null) return "";

        StringBuilder escaped = new StringBuilder();

        for (int index = 0; index < value.length(); index += 1) {
            char character = value.charAt(index);

            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }

        return escaped.toString();
    }
}
