# Club Plus - Backend Java Spring Boot

## Description

Ce projet constitue la partie serveur (backend) de l'application Club Plus. Développé en Java avec le framework Spring
Boot, il expose une API RESTful qui sert de point d'entrée unique pour toutes les interactions initiées par le frontend
Angular ou d'autres clients potentiels. Le backend implémente toute la logique métier de l'application, la gestion des
données et la sécurité. Il communique avec le frontend en utilisant le format JSON.

L'objectif principal est de fournir un service robuste, sécurisé et performant pour la gestion des réservations
d'événements sportifs, des utilisateurs, des clubs, et des fonctionnalités sociales associées.

## Fonctionnalités Principales (via API RESTful)

Le backend expose des endpoints pour gérer les aspects suivants de l'application:

* **Gestion des Utilisateurs (Membres, Gestionnaires, Administrateurs):**
    * Authentification (login via `/api/auth/login` générant un token JWT) et gestion des sessions.
    * Création de comptes (inscription de membres et de structures/clubs).
    * Gestion des profils utilisateurs (modification des informations personnelles, mot de passe).
    * Gestion des rôles (MEMBRE, RESERVATION, ADMIN).
* **Gestion des Clubs:**
    * Création et modification des informations du club (pour les administrateurs).
    * Gestion des adhésions des membres aux clubs.
* **Gestion des Événements:**
    * Opérations CRUD complètes (Créer, Lire, Mettre à jour, Supprimer) pour les événements.
    * Gestion du planning et des types d'événements.
    * Gestion des catégories de places pour les événements.
* **Système de Réservation:**
    * Création, consultation et annulation de réservations.
    * Validation des réservations (limite de places par membre, disponibilité).
    * Suivi des présences (potentiellement via la génération et la validation de QR codes).
* **Fonctionnalités Sociales:**
    * Gestion des demandes d'amis (envoi, acceptation, refus).
    * Consultation de listes d'amis.
* **Notation:**
    * Permettre aux utilisateurs de noter les événements participés.
    * Consultation des notations.
* **Tableaux de Bord et Statistiques:**
    * Fourniture de données pour les tableaux de bord (statistiques club, événements, etc.).

## Architecture Backend

L'application backend est structurée suivant une architecture en couches, favorisant la séparation des préoccupations (
Separation of Concerns), la testabilité et la maintenabilité:

* **Couche Présentation (Web/Controller):**
    * Implémentée avec Spring MVC.
    * Gère les requêtes HTTP entrantes et formule les réponses HTTP.
    * Expose les endpoints de l'API REST.
    * Reçoit les données (souvent sous forme de Data Transfer Objects - DTOs).
    * Effectue une première validation des entrées (via `@Valid`).
    * Délègue le traitement à la couche Service.
    * Transforme les résultats métier en réponses JSON (avec contrôle via `@JsonView`).
* **Couche Métier (Service):**
    * Contient la logique métier principale de l'application (`@Service`).
    * Orchestre les fonctionnalités et les cas d'utilisation.
    * Interagit avec la couche d'accès aux données (Repositories).
    * Applique les règles de gestion complexes.
    * Gère les transactions (via `@Transactional`).
* **Couche d'Accès aux Données (Repository/Persistence):**
    * Responsable de toute interaction avec la base de données.
    * Implémentée avec Spring Data JPA et Hibernate comme ORM.
    * Les interfaces Repository (`@Repository`, étendant `JpaRepository`) définissent les opérations CRUD et les
      requêtes personnalisées (JPQL ou Query Methods) sur les entités JPA (`@Entity`).

## Technologies et Frameworks

* **Langage de Programmation:** Java 17 (LTS).
* **Framework Principal:** Spring Boot.
* **Accès aux Données:** Spring Data JPA, Hibernate (en tant que fournisseur JPA).
* **Gestion de la Sécurité:** Spring Security (pour l'authentification JWT, RBAC, etc.).
* **API REST:** Spring MVC.
* **Utilitaires:** Project Lombok (pour réduire le code répétitif).
* **Gestion de Projet et Dépendances:** Apache Maven.
* **Base de Données:** MySQL (exécutée dans un conteneur Docker pour le développement).

## API RESTful

* **Protocole et Format:** Communication via HTTP(S), format d'échange de données JSON (`application/json`).
* **Principes REST:** L'API est conçue pour être stateless. Les ressources sont identifiées par des URI claires et les
  opérations utilisent les méthodes HTTP standard (GET, POST, PUT, DELETE).
* **Authentification:** L'accès aux ressources protégées est sécurisé via des Tokens Web JSON (JWT). Après
  authentification réussie (endpoint `/api/auth/login`), le client reçoit un JWT à inclure dans l'en-tête
  `Authorization` (schéma `Bearer`) des requêtes suivantes.
* **Gestion des Réponses:** Utilisation des codes de statut HTTP standard pour indiquer le résultat des opérations (200,
  201, 400, 401, 403, 404, 409, 500).
* **Contrôle des Données Exposées:** Utilisation des annotations `@JsonView` pour définir des profils de sérialisation
  JSON différents selon le contexte, évitant l'exposition involontaire d'informations sensibles.

## Sécurité

Plusieurs mesures de sécurité sont implémentées:

* **Authentification et Gestion de Session:** Basée sur les tokens JWT signés avec une durée de validité limitée.
* **Protection des Mots de Passe:** Hachage systématique des mots de passe avant stockage en base de données avec
  l'algorithme BCrypt (via Spring Security).
* **Contrôle d'Accès Basé sur les Rôles (RBAC):** Application stricte des rôles (MEMBRE, RESERVATION, ADMIN) via des
  annotations Spring Security standard (`@PreAuthorize`) et des annotations personnalisées (`@IsMembre`,
  `@IsReservation`, `@IsAdmin`, `@IsConnected`).
* **Contrôle d'Accès au Niveau des Données:** Vérifications dans la couche métier pour s'assurer que les utilisateurs n'
  accèdent/modifient que les données les concernant ou relevant de leur périmètre.
* **Validation des Entrées:** Validation systématique des données côté serveur (`@Valid` et contraintes comme
  `@NotNull`, `@Size`).
* **Prévention des Injections SQL:** L'utilisation de JPA/Hibernate avec des requêtes paramétrées protège contre les
  injections SQL.
* **Sécurisation du Transport (Prévue):** Déploiement prévu avec HTTPS (SSL/TLS) pour chiffrer les communications.

## Prérequis

* JDK 17 (ou supérieur compatible)
* Apache Maven 3.6.x (ou supérieur)
* MySQL Server (ou une instance Docker de MySQL)
* Un outil pour tester les API (ex: Postman)

## Installation

1. Clonez le dépôt du projet backend :
   ```
   git clone https://github.com/Karmadibsa/ClubPlusBackEnd
   ```
3. Configurez la base de données :
    * Assurez-vous que votre instance MySQL est en cours d'exécution.
    * Modifiez le fichier `src/main/resources/application.properties` (ou `application.yml`) pour configurer les
      informations de connexion à votre base de données (URL, nom d'utilisateur, mot de passe). Exemple :
      ```
      spring.datasource.url=jdbc:mysql://localhost:3306/clubplus_db?createDatabaseIfNotExist=true
      spring.datasource.username=root
      spring.datasource.password=votremotdepasse
      spring.jpa.hibernate.ddl-auto=update # ou validate en production
      spring.jpa.show-sql=true
      ```
4. Compilez et packagez l'application avec Maven :
   ```
   mvn clean package
   ```

## Lancement de l'application

Après avoir packagé l'application (un fichier `.jar` sera généré dans le répertoire `target`), vous pouvez la lancer
avec la commande :

   ```
    java -jar target/nom-de-votre-application.jar
   ```

text
Remplacez `nom-de-votre-application.jar` par le nom réel du fichier JAR généré.
Par défaut, l'application démarrera sur le port `8080` (configurable dans `application.properties`).

## Endpoints API Principaux (Exemples)

* `POST /api/auth/login` : Authentification d'un utilisateur.
* `POST /api/auth/register` : Inscription d'un nouveau membre.
* `GET /api/events` : Liste des événements (accès soumis à authentification et rôles).
* `POST /api/reservations` : Création d'une réservation.
* `GET /api/users/me` : Informations sur l'utilisateur connecté.

Consultez la documentation d'API (si disponible, ex: Swagger/OpenAPI) ou le code des contrôleurs pour la liste
exhaustive des endpoints.

## Tests

Pour lancer les tests unitaires et d'intégration (si configurés) :

   ```
    mvn test
   ```

## Auteur

* **Axel MOMPER** dans le cadre du Projet Fil rouge de la formation de Concepteur Developpeur d'application à Metz
  Numeric School

## Lien du Front-end

   ```
    https://github.com/Karmadibsa/ClubPlusFrontEnd
   ```

