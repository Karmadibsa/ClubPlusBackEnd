-- =============================================
-- == INSERTION DES DONNÉES DE TEST (CORRIGÉES) ==
-- =============================================

-- 1. Insertion des Membres (AVEC RÔLE et hash BCrypt $2a$)
-- Le hash pour 'password' est $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO membre (nom, prenom, date_naissance, date_inscription, numero_voie, rue, codepostal, ville, telephone,
                    email, password, role, actif, anonymize_date)
VALUES ('AdminClub', 'Alice', '1988-03-10', '2024-01-10', '10', 'Rue Principale', '75001', 'Paris', '0601010101',
        'alice.admin@club.fr', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG',
        'ADMIN', 1, NULL),       -- ID: 1 (Devient ADMIN du Club 1)
       ('ResaClub', 'Charlie', '1992-07-01', '2024-08-15', '3', 'Boulevard Central', '75006', 'Paris', '0603030303',
        'charlie.resa@club.fr', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG',
        'RESERVATION', 1, NULL), -- ID: 2 (Devient RESERVATION du Club 1)
       ('MembreSimple', 'Bob', '1995-11-25', '2024-05-20', '25', 'Avenue Secondaire', '75008', 'Paris', '0602020202',
        'bob.membre@email.com', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG',
        'MEMBRE', 1, NULL),      -- ID: 3 (Simple MEMBRE du Club 1)
       ('AutreMembre', 'David', '1980-01-05', '2025-02-01', '44', 'Rue du Quartier', '75010', 'Paris', '0604040404',
        'david.autre@email.com', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG',
        'MEMBRE', 1, NULL),      -- ID: 4 (Simple MEMBRE du Club 1)
       ('MembreMultiClub', 'Eva', '2000-09-30', '2025-04-05', '5', 'Quai Est', '75010', 'Paris', '0605050505',
        'eva.multi@email.com', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG',
        'MEMBRE', 1, NULL),      -- ID: 5 (Membre des Clubs 1 et 2)
       ('AdminClub2', 'Franck', '1990-04-15', '2025-01-01', '100', 'Grand Rue', '69001', 'Lyon', '0606060606',
        'franck.admin@club2.fr', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG', 'ADMIN', 1, NULL),
-- ID: 6 (Admin du Club 2)
       ('Membre Inactif', 'Ancien', '1975-01-01', '2023-01-15', '99', 'Rue Perdue', '99999', 'Inconnu', '0123456789',
        'deleted+id7@example.com', 'invalid_hash', 'MEMBRE', -- Données anonymisées
        0, '2025-01-10 15:30:00');

-- 2. Insertion des Clubs (SANS admin_id)
INSERT INTO club (nom, date_creation, date_inscription, numero_voie, rue, codepostal, ville, telephone, email, actif,
                  desactivation_date)
VALUES ('Club Omnisports Paris Centre', '2010-01-15', '2024-01-10', '1', 'Place du Sport', '75001', 'Paris',
        '0102030405', 'contact@csc-paris.fr', 1, NULL), -- ID: 1
       ('Lyon Padel Club', '2018-06-01', '2025-01-01', '200', 'Avenue Padel', '69007', 'Lyon', '0477889900',
        'contact@lyonpadel.fr', 1, NULL),
-- ID: 2
       ('[Désactivé] Ancien Club Test', '2005-05-05', '2022-01-01', '12', 'Rue Oubliée', '12345', 'TestVille',
        '0000000000', 'inactive+3@clubplus.invalid', -- Email modifié
        0, '2024-11-15 10:00:00');
-- Club 3 inactif
-- Note: code_club sera généré par @PostPersist lors du démarrage de l'app avec ces données.

-- 3. Insertion des Adhésions (SANS role_dans_club)
-- Lie les membres aux clubs. Le rôle est défini dans la table 'membre'.
INSERT INTO adhesion (membre_id, club_id, date_adhesion)
VALUES (1, 1, '2024-01-10 10:00:00'), -- Alice (ADMIN) -> Club 1
       (2, 1, '2024-08-15 11:00:00'), -- Charlie (RESERVATION) -> Club 1
       (3, 1, '2024-05-20 12:00:00'), -- Bob (MEMBRE) -> Club 1
       (4, 1, '2025-02-01 13:00:00'), -- David (MEMBRE) -> Club 1
       (5, 1, '2025-04-05 14:00:00'), -- Eva (MEMBRE) -> Club 1
       (5, 2, '2025-04-06 15:00:00'), -- Eva (MEMBRE) -> Club 2 AUSSI
       (6, 2, '2025-01-01 09:00:00');
-- Franck (ADMIN) -> Club 2

-- 4. Insertion des Événements (Vérifier les organisateur_id)
INSERT INTO events (nom, start, end, description, location, organisateur_id, actif, desactivation_date)
VALUES ('Tournoi Badminton CSC (Passé)', '2025-02-15 14:00:00', '2025-02-15 18:00:00', 'Tournoi amical interne.',
        'Gymnase Sud', 1,
        1, NULL), -- ID: 1
       ('Soirée CSC Mars (Passé)', '2025-03-20 19:00:00', '2025-03-20 23:00:00', 'Rencontre membres.', 'Club House',
        1,
        1, NULL), -- ID: 2
       ('Tournoi Padel Lyon (Futur)', '2025-06-07 10:00:00', '2025-06-07 17:00:00', 'Tournoi P100 Hommes.',
        'Lyon Padel Est', 2,
        1, NULL), -- ID: 3
       ('Initiation Escalade CSC (Futur)', '2025-05-18 14:00:00', '2025-05-18 16:00:00', 'Découverte salle.',
        'Salle GrimpUp', 1,
        1, NULL),
-- ID: 4
       ('[Annulé] Stage Tennis CSC (Futur)', '2025-07-10 09:00:00', '2025-07-12 17:00:00', 'Stage intensif.',
        'Courts Central', 1,
        0, '2025-04-15 09:30:00');
-- Événement futur ANNULÉ

-- 5. Insertion des Catégories (Vérifier les event_id)
INSERT INTO categories (event_id, nom, capacite)
VALUES (1, 'Simple H', 16),   -- cat ID: 1
       (1, 'Simple F', 16),   -- cat ID: 2
       (2, 'Buffet', 50),     -- cat ID: 3
       (3, 'P100 Hommes', 24),-- cat ID: 4
       (4, 'Débutant', 10),
       (5, 'Prout', 100);
-- cat ID: 5

-- 6. Insertion des Réservations (Vérifier membre_id, event_id, categorie_id)
-- Utiliser l'UUID généré par Java ou laisser NULL si la DB peut le générer ?
-- Le plus simple est de laisser @PrePersist le faire. Ne pas mettre en SQL.
-- Supposons que la table reservation s'appelle 'reservation'
INSERT INTO reservations (membre_id, event_id, categorie_id, date_reservation, reservation_uuid, status)
VALUES (1, 1, 1, '2025-02-01 10:00:00', UUID(), 'UTILISE'),  -- Alice (Admin) au tournoi passé
       (3, 1, 2, '2025-02-02 11:00:00', UUID(), 'UTILISE'),  -- Bob (Membre) au tournoi passé
       (1, 2, 3, '2025-03-01 10:00:00', UUID(), 'UTILISE'),  -- Alice (Admin) à la soirée passée
       (3, 2, 3, '2025-03-01 11:00:00', UUID(), 'UTILISE'),  -- Bob (Membre) à la soirée passée
       (6, 3, 4, '2025-05-01 09:00:00', UUID(), 'CONFIRME'), -- Franck (Admin C2) au tournoi Lyon
       (5, 3, 4, '2025-05-02 10:00:00', UUID(), 'CONFIRME'), -- Eva (Membre C2) au tournoi Lyon
       (1, 4, 5, '2025-04-20 14:00:00', UUID(), 'CONFIRME'),
-- Alice (Admin C1) à l'initiation escalade
       (4, 4, 5, '2025-04-21 15:00:00', UUID(), 'ANNULE'),   -- David initiation escalade, ANNULÉE --- AJOUT ---
       (5, 5, 6, '2025-04-10 10:00:00', UUID(), 'ANNULE');
-- Eva stage annulé, résa ANNULÉE aussi (logique) --- AJOUT ---

-- 7. Insertion des Demandes d'Amis (Vérifier envoyeur_id, recepteur_id)
INSERT INTO demande_ami (envoyeur_id, recepteur_id, statut, date_demande)
VALUES (1, 3, 'ACCEPTE', '2025-03-01 10:00:00'), -- Alice et Bob sont amis
       (1, 2, 'ATTENTE', '2025-04-01 11:00:00'), -- Alice demande Charlie
       (4, 1, 'ATTENTE', '2025-04-05 12:00:00'), -- David demande Alice
       (3, 4, 'ACCEPTE', '2025-03-15 13:00:00'), -- Bob et David sont amis
       (5, 3, 'REFUSE', '2025-04-10 14:00:00'),  -- Eva a demandé Bob, refusé
       (1, 6, 'ATTENTE', '2025-05-01 08:00:00');
-- Alice demande Franck (autre club)

-- 8. Insertion des Notations (Seulement pour événements passés et membres ayant réservé)
-- Supposons que la table notation s'appelle 'notation'
INSERT INTO notations (event_id, membre_id, ambiance, propreté, organisation, fair_play, niveau_joueurs, date_notation)
VALUES (1, 1, 5, 4, 5, 5, 4, '2025-02-16 10:00:00'), -- Alice note Tournoi CSC
       (1, 3, 4, 3, 4, 4, 3, '2025-02-16 11:00:00'), -- Bob note Tournoi CSC
       (2, 1, 5, 5, 4, 5, 3, '2025-03-21 10:00:00'), -- Alice note Soirée CSC
       (2, 3, 4, 4, 3, 4, 3, '2025-03-21 11:00:00'); -- Bob note Soirée CSC
