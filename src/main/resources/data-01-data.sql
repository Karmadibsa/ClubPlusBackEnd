-- 1. Insertion du Club (admin_id sera mis à jour plus tard)
INSERT INTO club (nom, date_creation, date_inscription, numero_voie, rue, codepostal, ville, telephone, email,
                  code_club)
VALUES ('Club Sportif Central', '2010-01-15', '2024-01-10', '1', 'Place du Sport', '75001', 'Paris', '0102030405',
        'contact@csc.fr', 'CLUB-0001');
-- Supposons que cet INSERT génère l'ID 1 pour le club.

-- 2. Insertion des Membres (associés au club ID 1)
-- Attention au hash du mot de passe ! Remplacer si nécessaire. Tous ont le même MDP ici.
INSERT INTO membre (nom, prenom, date_naissance, date_inscription, numero_voie, rue, codepostal, ville, telephone,
                    email, password, role, club_id)
VALUES ('Admin', 'Alice', '1988-03-10', '2024-01-10', '10', 'Rue de Rivoli', '75001', 'Paris', '0601010101',
        'alice.admin@csc.fr', '$2y$10$Z8qF28Ev3TDElZ8f8Jq1cuqYPpL/LgD0O1DLAXRivxamdVqP5hVuq', 'ADMIN',
        1), -- ID supposé: 1
       ('Membre', 'Bob', '1995-11-25', '2024-05-20', '25', 'Avenue des Champs', '75008', 'Paris', '0602020202',
        'bob.membre@email.com', '$2y$10$Z8qF28Ev3TDElZ8f8Jq1cuqYPpL/LgD0O1DLAXRivxamdVqP5hVuq', 'MEMBRE',
        1), -- ID supposé: 2
       ('Resa', 'Charlie', '1992-07-01', '2024-08-15', '3', 'Boulevard St-Germain', '75006', 'Paris', '0603030303',
        'charlie.resa@email.com', '$2y$10$Z8qF28Ev3TDElZ8f8Jq1cuqYPpL/LgD0O1DLAXRivxamdVqP5hVuq', 'RESERVATION',
        1), -- ID supposé: 3
       ('Ancien', 'David', '1980-01-05', '2025-02-01', '44', 'Rue du Faubourg', '75010', 'Paris', '0604040404',
        'david.ancien@email.com', '$2y$10$Z8qF28Ev3TDElZ8f8Jq1cuqYPpL/LgD0O1DLAXRivxamdVqP5hVuq', 'MEMBRE',
        1), -- ID supposé: 4
       ('Nouveau', 'Eva', '2000-09-30', '2025-04-05', '5', 'Quai de Valmy', '75010', 'Paris', '0605050505',
        'eva.nouveau@email.com', '$2y$10$Z8qF28Ev3TDElZ8f8Jq1cuqYPpL/LgD0O1DLAXRivxamdVqP5hVuq', 'MEMBRE', 1);
-- ID supposé: 5

-- 3. Mise à jour de l'admin du Club
UPDATE club
SET admin_id = 1 -- Alice Admin (ID membre supposé 1)
WHERE id = 1;
-- Club ID 1

-- 4. Insertion des Événements (organisés par le club ID 1)
-- Inclure des événements passés et futurs
INSERT INTO events (nom, start, end, description, location, organisateur_id)
VALUES ('Tournoi Badminton Passé', '2025-02-15 14:00:00', '2025-02-15 18:00:00', 'Tournoi amical interne.',
        'Gymnase Sud', 1),     -- ID supposé: 1 (PASSÉ)
       ('Stage Tennis Février', '2025-02-22 09:00:00', '2025-02-24 17:00:00', 'Stage intensif weekend.',
        'Courts Centraux', 1), -- ID supposé: 2 (PASSÉ)
       ('Soirée Club Mars', '2025-03-20 19:00:00', '2025-03-20 23:00:00', 'Rencontre conviviale des membres.',
        'Club House', 1),      -- ID supposé: 3 (PASSÉ)
       ('Compétition Régionale Judo', '2025-04-05 08:00:00', '2025-04-06 19:00:00', 'Compétition officielle.',
        'Dojo Régional', 1),   -- ID supposé: 4 (PASSÉ)
       ('Sortie Vélo Mai', '2025-05-10 09:00:00', '2025-05-10 13:00:00', 'Balade dans la forêt de Marly.',
        'Parking Forêt', 1),   -- ID supposé: 5 (FUTUR - DANS 30J)
       ('Tournoi Foot Juin', '2025-06-07 10:00:00', '2025-06-07 17:00:00', 'Tournoi 7x7 inter-équipes.',
        'Stade Municipal', 1), -- ID supposé: 6 (FUTUR)
       ('Initiation Escalade Mai', '2025-05-18 14:00:00', '2025-05-18 16:00:00', 'Découverte de l''escalade en salle.',
        'Salle GrimpUp', 1);
-- ID supposé: 7 (FUTUR - DANS 30J)

-- 5. Insertion des Catégories pour les événements
INSERT INTO categories (event_id, nom, capacite)
VALUES
-- Event 1 (Badminton Passé) - ID supposé 1
(1, 'Simple Homme', 16),   -- ID Catégorie supposé: 1
(1, 'Simple Femme', 16),   -- ID Catégorie supposé: 2
(1, 'Double Mixte', 20),   -- ID Catégorie supposé: 3 (capa 10 paires)
-- Event 2 (Tennis Février) - ID supposé 2
(2, 'Court 1', 4),         -- ID Catégorie supposé: 4
(2, 'Court 2', 4),         -- ID Catégorie supposé: 5
-- Event 3 (Soirée Mars) - ID supposé 3
(3, 'Entrée Générale', 50),-- ID Catégorie supposé: 6
-- Event 4 (Judo Avril) - ID supposé 4
(4, 'Moins de 60kg', 10),  -- ID Catégorie supposé: 7
(4, 'Moins de 70kg', 10),  -- ID Catégorie supposé: 8
(4, 'Spectateur', 30),     -- ID Catégorie supposé: 9
-- Event 5 (Vélo Mai) - ID supposé 5
(5, 'Groupe Loisir', 15),  -- ID Catégorie supposé: 10
(5, 'Groupe Sportif', 10),-- ID Catégorie supposé: 11
-- Event 6 (Foot Juin) - ID supposé 6
(6, 'Équipe Inscrite', 12),-- ID Catégorie supposé: 12 (capacité en nb d'équipes)
-- Event 7 (Escalade Mai) - ID supposé 7
(7, 'Débutant Adulte', 8), -- ID Catégorie supposé: 13
(7, 'Débutant Enfant', 8);
-- ID Catégorie supposé: 14

-- 6. Insertion des Réservations
INSERT INTO reservations (membre_id, event_id, categorie_id, date_reservation)
VALUES
-- Event 1 (Badminton Passé)
(1, 1, 1, '2025-02-01 10:00:00'),  -- Alice
(2, 1, 2, '2025-02-02 11:00:00'),  -- Bob
(4, 1, 1, '2025-02-03 12:00:00'),  -- David
-- Event 3 (Soirée Mars)
(1, 3, 6, '2025-03-01 10:00:00'),  -- Alice
(2, 3, 6, '2025-03-01 11:00:00'),  -- Bob
(3, 3, 6, '2025-03-02 12:00:00'),  -- Charlie
(4, 3, 6, '2025-03-03 13:00:00'),  -- David
-- Event 4 (Judo Avril)
(4, 4, 7, '2025-03-15 10:00:00'),  -- David (-60kg)
(1, 4, 9, '2025-03-16 11:00:00'),  -- Alice (Spectateur)
-- Event 5 (Vélo Mai - FUTUR)
(2, 5, 10, '2025-04-10 09:00:00'), -- Bob (Loisir) - 1ère résa
(2, 5, 10, '2025-04-11 10:00:00'), -- Bob (Loisir) - 2ème résa (pour tester limite 2)
(5, 5, 11, '2025-04-11 11:00:00'), -- Eva (Sportif)
-- Event 7 (Escalade Mai - FUTUR)
(1, 7, 13, '2025-04-12 10:00:00'), -- Alice (Adulte)
(5, 7, 13, '2025-04-12 11:00:00');
-- Eva (Adulte)

-- 7. Insertion des Demandes d'Amis
INSERT INTO demande_ami (envoyeur_id, recepteur_id, statut, date_demande)
VALUES (1, 2, 'ACCEPTE', '2025-03-01 10:00:00'), -- Alice et Bob sont amis
       (1, 3, 'ATTENTE', '2025-04-01 11:00:00'), -- Alice demande Charlie
       (4, 1, 'ATTENTE', '2025-04-05 12:00:00'), -- David demande Alice
       (2, 4, 'ACCEPTE', '2025-03-15 13:00:00'), -- Bob et David sont amis
       (5, 2, 'REFUSE', '2025-04-10 14:00:00');
-- Eva a demandé Bob, qui a refusé

-- 8. Insertion des Notations (pour les événements PASSÉS)
-- Note: Mettre la date manuellement si @PrePersist n'est pas fiable avec SQL direct
INSERT INTO notation (event_id, membre_id, ambiance, propreté, organisation, fair_play, niveau_joueurs, date_notation)
VALUES
-- Event 1 (Badminton Passé)
(1, 1, 5, 4, 5, 5, 4, '2025-02-16 10:00:00'), -- Note d'Alice
(1, 2, 4, 3, 4, 4, 3, '2025-02-16 11:00:00'), -- Note de Bob
(1, 4, 5, 5, 5, 5, 5, '2025-02-17 12:00:00'), -- Note de David
-- Event 3 (Soirée Mars)
(3, 1, 5, 5, 4, 5, 3, '2025-03-21 10:00:00'), -- Note d'Alice (Niveau joueur moins pertinent ici)
(3, 2, 4, 4, 3, 4, 3, '2025-03-21 11:00:00'), -- Note de Bob
-- Event 4 (Judo Avril)
(4, 4, 3, 4, 3, 5, 5, '2025-04-07 10:00:00'), -- Note de David (participant)
(4, 1, 4, 5, 4, 4, 4, '2025-04-07 11:00:00'); -- Note d'Alice (spectatrice)

