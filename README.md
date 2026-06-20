# ⚙️ GestionProject_Back (Backend TrackFlow)

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.6-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Spring Security](https://img.shields.io/badge/Spring_Security-Stateless_JWT-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-security)

**GestionProject_Back** est l'API REST de la plateforme de gestion de projet Agile **TrackFlow**. Développé en **Java 21** avec **Spring Boot 4.0.6**, ce backend prend en charge l'authentification sécurisée, la gestion des structures organisationnelles (entreprises et projets), le suivi des sprints, l'affectation des tâches via un tableau Kanban et le calcul d'indicateurs de performance Scrum (vélocité, Lead Time et Cycle Time).

---

##  Fonctionnalités Principales

*   **Sécurité \& Authentification** :
    *   Inscription avec hachage de mot de passe via BCrypt.
    *   Mécanisme de validation de compte par courriel (Confirmation token UUID expirable).
    *   Authentification sans état (stateless) avec génération de token **JWT** (durée d'expiration de 24h).
*   **Gestion des Organisations (Entreprises)** :
    *   Création d'entreprises (le créateur devient `ADMIN`).
    *   Système d'invitations de collaborateurs par courriel (statut `PENDING`, `ACCEPTED`, `REJECTED`).
*   **Gestion des Projets** :
    *   Projets personnels (`PERSONAL`) : actifs immédiatement à la création.
    *   Projets d'entreprise (`ENTREPRISE`) : créés en attente (`PENDING`) et soumis à l'approbation/rejet de l'administrateur de l'entreprise.
    *   Affectation automatique du rôle `MANAGER` au créateur du projet.
    *   Invitation directe de membres de l'entreprise dans l'équipe projet.
*   **Gestion des Sprints** :
    *   Planification de sprints (statut `PLANNED`).
    *   Rattachement exclusif des tâches au statut `TODO` au sprint (Sprint Backlog).
    *   Déclaration de la capacité disponible en heures pour chaque collaborateur par sprint.
    *   Règle métier stricte : **Un seul sprint actif (`ACTIVE`) par projet**.
*   **Suivi des Tâches (Backlog \& Kanban)** :
    *   Création de tâches avec priorité (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) et Story Points ($\ge 0$).
    *   Assignation à des collaborateurs validés sur le projet.
    *   Transition d'états Kanban (`TODO` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `DONE`).
    *   Horodatage automatique de la prise en charge (`dateDebutTravail`) et de la finalisation (`dateCompletion`) pour le calcul des métriques.
*   **Analytique \& Métriques Scrum** :
    *   **Vélocité** : Somme des Story Points complétés lors de la fermeture d'un sprint.
    *   **Lead Time moyen** : Temps moyen écoulé entre la création d'une tâche et sa finalisation (DONE).
    *   **Cycle Time moyen** : Temps moyen de développement effectif (de IN_PROGRESS à DONE).
    *   **Workload** : Répartition de la charge de travail (nombre de tâches par statut) par collaborateur.

---

##  Stack Technique

*   **Langage** : Java 21
*   **Framework Principal** : Spring Boot 4.0.6
*   **Sécurité** : Spring Security, JWT (io.jsonwebtoken v0.11.5)
*   **Accès aux Données** : Spring Data JPA, Hibernate
*   **Base de Données** : PostgreSQL
*   **Messagerie** : Spring Boot Starter Mail (SMTP)
*   **Outils de test** : JUnit 5, MockMvc, H2 Database (pour la base de test)
*   **Build Tool** : Maven

---

##  Prérequis

*   **Java Development Kit (JDK)** : Version 21 installé.
*   **PostgreSQL** : Un serveur PostgreSQL actif.
*   **Mailtrap** (ou autre serveur SMTP de test) : Pour intercepter les e-mails d'inscription et d'invitation.

---

##  Configuration de l'Application

La configuration s'effectue dans le fichier [application.properties](file:///c:/Projects/GestionProject_Back/src/main/resources/application.properties) :

### 1. Base de données PostgreSQL
Créer une base de données nommée `gestionprojet` dans PostgreSQL, puis configurer les propriétés :
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gestionprojet
spring.datasource.username=postgres
spring.datasource.password=${Postgress_password}
```
*(Vous devez définir la variable d'environnement `Postgress_password` ou remplacer directement par votre mot de passe).*

### 2. Configuration SMTP (Mailtrap par défaut)
Renseigner vos identifiants de boîte de test :
```properties
spring.mail.host=sandbox.smtp.mailtrap.io
spring.mail.port=587
spring.mail.username=VOTRE_USERNAME_MAILTRAP
spring.mail.password=VOTRE_PASSWORD_MAILTRAP
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### 3. Clé Secrète JWT
Définir la clé de signature du token (256-bits minimum) via la variable `jwt_secret` ou en dur :
```properties
jwt.secret=${jwt_secret}
jwt.expiration=86400000
```

---

##  Démarrage

Pour compiler et lancer l'application en mode développement :

```bash
# Sur Windows
mvnw.cmd spring-boot:run

# Sur Linux / macOS
chmod +x mvnw
./mvnw spring-boot:run
```

L'API sera disponible sur : `http://localhost:8080`

---

## Tests d'Intégration

Le projet inclut une suite de tests couvrant les parcours utilisateurs de bout en bout (utilisation d'une base H2 en mémoire pour les tests) :

Pour exécuter les tests :
```bash
mvn clean test
```

### Scénarios testés :
*   [UserScenariosTest](file:///c:/Projects/GestionProject_Back/src/test/java/com/ensao/gestionprojet/UserScenariosTest.java) : Inscription, validation de compte par e-mail, connexion, création d'entreprise et d'invitations.
*   [SprintScenariosTest](file:///c:/Projects/GestionProject_Back/src/test/java/com/ensao/gestionprojet/SprintScenariosTest.java) : Cycle de vie d'un sprint, affectation de tâches, planification de la disponibilité de l'équipe et règles métiers associées.
*   [DashboardAndKanbanScenariosTest](file:///c:/Projects/GestionProject_Back/src/test/java/com/ensao/gestionprojet/DashboardAndKanbanScenariosTest.java) : Kanban, calcul du Workload, et extraction dynamique des métriques de temps de flux (Lead/Cycle Time).

---

## 🌐 Documentation de l'API REST

Tous les points d'accès (à l'exception de `/api/auth/**`) nécessitent d'ajouter l'en-tête HTTP :  
`Authorization: Bearer <VOTRE_TOKEN_JWT>`

### Authentification publique (`/api/auth`)
*   `POST /api/auth/register` : Créer un compte utilisateur.
*   `GET /api/auth/confirm?token={token}` : Confirmer et activer le compte par e-mail.
*   `POST /api/auth/login` : Se connecter et récupérer le jeton JWT.

### Gestion Entreprises (`/api/entreprises`)
*   `POST /api/entreprises` : Créer une entreprise (créateur devient ADMIN).
*   `POST /api/entreprises/{id}/invite` : Inviter un membre à rejoindre l'entreprise (ADMIN uniquement).
*   `POST /api/entreprises/invitations/{id}/accept` : Accepter une invitation d'entreprise.
*   `POST /api/entreprises/invitations/{id}/reject` : Refuser une invitation d'entreprise.

### Gestion Projets (`/api/projets`)
*   `POST /api/projets` : Créer un projet (type `PERSONAL` ou `ENTREPRISE`).
*   `PUT /api/projets/{id}/valider` : Valider un projet d'entreprise en attente (ADMIN de l'entreprise uniquement).
*   `PUT /api/projets/{id}/rejeter` : Rejeter un projet d'entreprise (ADMIN de l'entreprise uniquement).
*   `GET /api/projets/mes-projets` : Récupérer les projets où l'utilisateur connecté est membre actif.
*   `POST /api/projets/{id}/invite` : Inviter directement un membre de l'entreprise dans le projet (MANAGER uniquement).

### Sprints (`/api/sprints`)
*   `POST /api/sprints` : Créer un sprint planned (MANAGER uniquement).
*   `POST /api/sprints/{id}/taches` : Ajouter des tâches TODO au sprint backlog (MANAGER uniquement).
*   `POST /api/sprints/{id}/disponibilites` : Spécifier la capacité disponible (en heures) de l'équipe (MANAGER uniquement).
*   `GET /api/sprints/projet/{projetId}` : Liste des sprints d'un projet.

### Tâches (`/api/taches`)
*   `POST /api/taches` : Créer une tâche dans le backlog (MANAGER uniquement).
*   `PUT /api/taches/{id}/assigner/{userId}` : Assigner une tâche à un collaborateur (MANAGER uniquement).
*   `PUT /api/taches/{id}/statut` : Mettre à jour le statut (`TODO`, `IN_PROGRESS`, `DONE`) (MANAGER ou membre assigné).
*   `GET /api/taches/backlog/{projetId}` : Consulter le backlog produit (tâches non planifiées).
*   `GET /api/taches/kanban/{projetId}` : Récupérer le tableau Kanban du projet.

### Tableau de bord \& Métriques (`/api/dashboard`)
*   `GET /api/dashboard/{projetId}/workload` : Obtenir la répartition des tâches par membre.
*   `GET /api/dashboard/{projetId}/metriques` : Calculer les moyennes de Lead Time et Cycle Time (en heures) des tâches terminées.
