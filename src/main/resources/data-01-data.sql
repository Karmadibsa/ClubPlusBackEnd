-- ========================================================
-- == INSERTION DONNÉES DE TEST (NETTOYÉES ET COHÉRENTES) ==
-- ========================================================
-- Règles appliquées:
-- - Seuls les MEMBREs peuvent être amis.
-- - Seuls les MEMBREs peuvent créer des réservations (via API testée).
-- - Les réservations/amitiés existantes impliquant ADMIN/RESERVATION sont supprimées.

-- 1. MEMBRES (Inchangé)
-- Rappel: Alice(1, ADMIN C1), Charlie(2, RESA C1), Bob(3, MEMBRE C1), David(4, MEMBRE C1), Eva(5, MEMBRE C1&C2), Franck(6, ADMIN C2), Ancien(7, Inactif), Gerard(8, ADMIN C4), Helene(9, MEMBRE C4), Testeur(10, MEMBRE C1)
-- Hash pour 'password': $2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG
INSERT INTO membre (id, nom, prenom, date_naissance, date_inscription, numero_voie, rue, codepostal, ville, telephone,
                    email, password, role, actif, anonymize_date)
VALUES (1, 'AdminClub', 'Alice', '1988-03-10', '2024-01-10', '10', 'Rue Principale', '75001', 'Paris', '0601010101',
        'alice.admin@club.fr', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG', 'ADMIN', 1, NULL),
       (2, 'ResaClub', 'Charlie', '1992-07-01', '2024-08-15', '3', 'Boulevard Central', '75006', 'Paris', '0603030303',
        'charlie.resa@club.fr', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG', 'RESERVATION', 1, NULL),
       (3, 'MembreSimple', 'Bob', '1995-11-25', '2024-05-20', '25', 'Avenue Secondaire', '75008', 'Paris', '0602020202',
        'bob.membre@email.com', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG', 'MEMBRE', 1, NULL),
       (4, 'AutreMembre', 'David', '1980-01-05', '2025-02-01', '44', 'Rue du Quartier', '75010', 'Paris', '0604040404',
        'david.autre@email.com', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG', 'MEMBRE', 1, NULL),
       (5, 'MembreMultiClub', 'Eva', '2000-09-30', '2025-04-05', '5', 'Quai Est', '75010', 'Paris', '0605050505',
        'eva.multi@email.com', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG', 'MEMBRE', 1, NULL),
       (6, 'AdminClub2', 'Franck', '1990-04-15', '2025-01-01', '100', 'Grand Rue', '69001', 'Lyon', '0606060606',
        'franck.admin@club2.fr', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG', 'ADMIN', 1, NULL),
       (7, 'Membre Inactif', 'Ancien', '1975-01-01', '2023-01-15', '99', 'Rue Perdue', '99999', 'Inconnu', '0123456789',
        'deleted+id7@example.com', 'invalid_hash', 'MEMBRE', 0, '2025-01-10 15:30:00'),
       (8, 'AdminClub3', 'Gerard', '1985-06-20', '2025-03-01', '7', 'Place Ouest', '33000', 'Bordeaux', '0608080808',
        'gerard.admin@club3.fr', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG', 'ADMIN', 1, NULL),
       (9, 'MembreClub3', 'Helene', '1998-12-12', '2025-03-05', '8', 'Cours Sud', '33000', 'Bordeaux', '0609090909',
        'helene.membre@club3.fr', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG', 'MEMBRE', 1, NULL),
       (10, 'MembrePourDelete', 'Testeur', '2001-01-01', '2025-04-01', '111', 'Rue Delete', '75011', 'Paris',
        '0610101010', 'testeur.delete@mail.com', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG',
        'MEMBRE', 1, NULL),
       (11, 'Nouveau', 'Gaston', '1998-05-12', '2025-04-10', '15', 'Rue des Lilas', '75019', 'Paris', '0611111111',
        'gaston.nouveau@email.com', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG', 'MEMBRE', 1, NULL),
       (12, 'Martin', 'Hélène', '2001-02-20', '2025-04-11', '22', 'Avenue Gambetta', '75020', 'Paris', '0612121212',
        'helene.martin@email.com', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG', 'MEMBRE', 1, NULL),
       (13, 'Dupont', 'Isabelle', '1985-10-03', '2025-04-15', '8', 'Boulevard Voltaire', '75011', 'Paris', '0613131313',
        'isabelle.dupont@email.com', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG', 'MEMBRE', 1, NULL),
       (14, 'Petit', 'Jean', '1993-07-18', '2025-04-20', '1', 'Rue de la Paix', '75002', 'Paris', '0614141414',
        'jean.petit@email.com', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG', 'MEMBRE', 0,
        '2025-04-25 10:00:00'), -- Membre inactif avec date
       (15, 'Moreau', 'Karim', '1999-12-01', '2025-04-22', '55', 'Rue du Faubourg Saint-Honoré', '75008', 'Paris',
        '0615151515', 'karim.moreau@email.com', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG',
        'MEMBRE', 1, NULL);

-- 2. CLUBS (Inchangé)
INSERT INTO club (id, nom, date_creation, date_inscription, numero_voie, rue, codepostal, ville, telephone, email,
                  actif, desactivation_date, code_club)
VALUES (1, 'Club Omnisports Paris Centre', '2010-01-15', '2024-01-10', '1', 'Place du Sport', '75001', 'Paris',
        '0102030405', 'contact@csc-paris.fr', 1, NULL, 'CLUB-0001'),
       (2, 'Lyon Padel Club', '2018-06-01', '2025-01-01', '200', 'Avenue Padel', '69007', 'Lyon', '0477889900',
        'contact@lyonpadel.fr', 1, NULL, 'CLUB-0002');

-- 3. ADHESIONS (Inchangé - Associe utilisateurs aux clubs, rôles définis dans membre)
INSERT INTO adhesion (membre_id, club_id, date_adhesion)
VALUES (1, 1, '2024-01-10 10:00:00'),  -- Alice (ADMIN) -> Club 1
       (2, 1, '2024-08-15 11:00:00'),  -- Charlie (RESA) -> Club 1
       (3, 1, '2024-05-20 12:00:00'),  -- Bob (MEMBRE) -> Club 1
       (4, 1, '2025-02-01 13:00:00'),  -- David (MEMBRE) -> Club 1
       (5, 1, '2025-04-05 14:00:00'),  -- Eva (MEMBRE) -> Club 1
       (5, 2, '2025-04-06 15:00:00'),  -- Eva (MEMBRE) -> Club 2
       (6, 2, '2025-01-01 09:00:00'),  -- Franck (ADMIN) -> Club 2
       (10, 1, '2025-04-01 12:00:00'),
       -- Adhésions pour les nouveaux membres (principalement Club 1)
       (11, 1, '2025-04-10 09:00:00'), -- Gaston -> Club 1
       (12, 1, '2025-04-11 10:00:00'), -- Hélène -> Club 1
       (13, 1, '2025-04-15 11:00:00'), -- Isabelle -> Club 1
       (13, 2, '2025-04-16 12:00:00'), -- Isabelle -> Club 2 (Multi-club)
       (14, 1, '2025-04-20 13:00:00'), -- Jean (inactif) -> Club 1
       (15, 1, '2025-04-22 14:00:00');
-- Karim -> Club 1

-- 4. EVENTS (Dates Event 4 ajustées pour être en cours au 20 Avril)
INSERT INTO events (id, nom, start, end, description, location, organisateur_id, actif, desactivation_date)
VALUES (1, 'Tournoi Badminton CSC (Passé)', '2025-02-15 14:00:00', '2025-02-15 18:00:00', 'Tournoi amical interne.',
        'Gymnase Sud', 1, 1, NULL),
       (2, 'Soirée CSC Mars (Passé)', '2025-03-20 19:00:00', '2025-03-20 23:00:00', 'Rencontre membres.', 'Club House',
        1, 1, NULL),
       (3, 'Tournoi Padel Lyon (Futur)', '2025-06-07 10:00:00', '2025-06-07 17:00:00', 'Tournoi P100 Hommes.',
        'Lyon Padel Est', 2, 1, NULL),
       (4, 'Initiation Escalade CSC (En Cours)', '2025-04-21 14:00:00', '2025-04-21 18:00:00', 'Découverte salle.',
        'Salle GrimpUp', 1, 1, NULL), -- DATE MODIFIEE
       (5, '[Annulé] Stage Tennis CSC (Futur)', '2025-07-10 09:00:00', '2025-07-12 17:00:00', 'Stage intensif.',
        'Courts Central', 1, 0, '2025-04-15 09:30:00'),
       (6, 'Soirée Jeux Club Paris (Futur)', '2025-08-10 19:00:00', '2025-08-10 23:00:00', 'Rencontre amicale jeux.',
        'Salle Polyvalente', 1, 1, NULL),
       (7, 'Stage Padel Découverte Lyon (Passé)', '2025-03-01 10:00:00', '2025-03-01 14:00:00',
        'Initiation et découverte.', 'Lyon Padel Ouest', 2, 1, NULL),
       (10, 'Nettoyage Local CSC (Passé)', '2025-04-12 09:00:00', '2025-04-12 12:00:00',
        'Grand nettoyage de printemps.', 'Local Matériel', 1, 1, NULL),
       (11, 'Stage Yoga Découverte CSC (Futur)', '2025-05-10 10:00:00', '2025-05-10 12:00:00',
        'Initiation aux postures de base.', 'Salle Zen', 1, 1, NULL),
       (12, 'Tournoi Tennis Double Mixte CSC (Futur)', '2025-09-14 09:00:00', '2025-09-14 17:00:00',
        'Tournoi amical ouvert à tous les niveaux.', 'Courts Central', 1, 1, NULL),
       (13, 'Apéro Afterwork CSC (Passé)', '2025-04-01 18:30:00', '2025-04-01 21:00:00',
        'Moment convivial après le travail.', 'Bar du Club', 1, 1, NULL);

-- 5. CATEGORIES (Capacité Cat 5 ajustée pour Test 53)
INSERT INTO categories (id, event_id, nom, capacite)
VALUES (1, 1, 'Simple H', 16),
       (2, 1, 'Simple F', 16),
       (3, 2, 'Buffet', 50),
       (4, 3, 'P100 Hommes', 24),
       (5, 4, 'Débutant', 2),               -- Capacité 2 pour Test 53
       (6, 5, 'Stage Intensif', 10),
       (7, 4, 'Confirmés', 8),
       (8, 4, 'Avancés', 6),
       (9, 2, 'Boissons', 60),
       (10, 6, 'Jeux de Société', 20),
       (11, 7, 'Initiation Padel', 15),
       (15, 10, 'Equipe Nettoyage', 10),
       (16, 11, 'Tapis Yoga', 15),          -- Pour Stage Yoga (Event 11)
       (17, 12, 'Equipe Double Mixte', 12), -- Pour Tournoi Tennis (Event 12) - 12 équipes = 24 joueurs
       (18, 13, 'Participant Apéro', 40);
-- Pour Apéro Afterwork (Event 13)

-- 6. RESERVATIONS (NETTOYÉES : Uniquement MEMBREs)
INSERT INTO reservations (membre_id, event_id, categorie_id, date_reservation, reservation_uuid, status)
VALUES -- Réservations pour MEMBREs uniquement
       (3, 1, 2, '2025-02-02 11:00:00', UUID(), 'UTILISE'),     -- Bob @ Badminton (Passé)
       (3, 2, 3, '2025-03-01 11:00:00', UUID(), 'UTILISE'),     -- Bob @ Soirée Mars (Passé)
       (5, 3, 4, '2025-05-01 09:00:00', UUID(), 'CONFIRME'),    -- Eva @ Padel Lyon (Futur) - Retrait doublon
       (4, 4, 5, '2025-04-21 15:00:00', UUID(), 'ANNULE'),      -- David @ Escalade (Annulée)
       (5, 5, 6, '2025-04-10 10:00:00', UUID(), 'ANNULE'),      -- Eva @ Stage Annulé
       (5, 7, 11, '2025-02-25 10:00:00', UUID(), 'UTILISE'),    -- Eva @ Stage Padel Lyon (Passé)
       -- Ajout pour Test Capacité Pleine (Test 53)
       (3, 4, 5, '2025-04-20 15:00:00', UUID(), 'CONFIRME'),    -- Bob @ Escalade (Cat 5, Capa 2)
       (5, 4, 5, '2025-04-20 15:01:00', UUID(), 'CONFIRME'),
       (4, 1, 1, '2025-02-01 10:00:00', UUID(), 'UTILISE'),
       -- Event 6 (Futur)
       (11, 6, 10, '2025-05-01 10:00:00', UUID(), 'CONFIRME'),  -- Gaston @ Soirée Jeux
       (12, 6, 10, '2025-05-02 11:00:00', UUID(), 'CONFIRME'),  -- Hélène @ Soirée Jeux
       -- Event 10 (Passé)
       (13, 10, 15, '2025-04-10 09:00:00', UUID(), 'UTILISE'),  -- Isabelle @ Nettoyage
       (15, 10, 15, '2025-04-11 10:00:00', UUID(), 'UTILISE'),  -- Karim @ Nettoyage
       -- Event 11 (Futur)
       (3, 11, 16, '2025-04-27 14:00:00', UUID(), 'CONFIRME'),  -- Bob @ Stage Yoga
       (5, 11, 16, '2025-04-28 15:00:00', UUID(), 'CONFIRME'),  -- Eva @ Stage Yoga (sa 2e résa pour cet event)
       (11, 11, 16, '2025-04-29 16:00:00', UUID(), 'ANNULE'),   -- Gaston @ Stage Yoga (annulée)
       -- Event 12 (Futur)
       (4, 12, 17, '2025-06-01 10:00:00', UUID(), 'CONFIRME'),  -- David @ Tournoi Tennis
       (13, 12, 17, '2025-06-02 11:00:00', UUID(), 'CONFIRME'), -- Isabelle @ Tournoi Tennis
       (13, 12, 17, '2025-06-03 12:00:00', UUID(), 'CONFIRME'), -- Isabelle @ Tournoi Tennis (sa 2e résa)
       -- Event 13 (Passé)
       (3, 13, 18, '2025-03-30 10:00:00', UUID(), 'UTILISE'),   -- Bob @ Apéro
       (12, 13, 18, '2025-03-31 11:00:00', UUID(), 'UTILISE');
-- Hélène @ Apéro
-- David (4) a participé à Event 1 / Cat 1;
-- Eva @ Escalade (Cat 5, Capa 2)

-- 7. DEMANDES D'AMIS (NETTOYÉES : Uniquement entre MEMBREs)
INSERT INTO demande_ami (envoyeur_id, recepteur_id, statut, date_demande)
VALUES -- Interactions entre MEMBREs uniquement
       (3, 4, 'ACCEPTE', '2025-03-15 13:00:00'),   -- Bob (3) et David (4) sont amis
       (5, 3, 'REFUSE', '2025-04-10 14:00:00'),    -- Eva (5) a refusé Bob (3)
       (4, 5, 'ATTENTE', '2025-04-18 10:00:00'),   -- David (4) a demandé Eva (5)
       (9, 10, 'ATTENTE', '2025-04-19 11:00:00'),
       (5, 10, 'ATTENTE', '2025-04-20 17:00:00'),
       -- Nouvelles demandes (impliquant aussi 11, 12, 13, 15 qui sont au Club 1)
       (3, 11, 'ATTENTE', '2025-04-23 10:00:00'),  -- Bob (3) demande Gaston (11) (Club 1)
       (12, 4, 'ATTENTE', '2025-04-24 11:00:00'),  -- Hélène (12) demande David (4) (Club 1)
       (13, 15, 'ACCEPTE', '2025-04-25 12:00:00'), -- Isabelle (13) et Karim (15) sont amis (Club 1)
       (5, 13, 'ATTENTE', '2025-04-26 09:00:00');
-- Eva (5) demande Isabelle (13) (Club 1 ou 2, ici Club 1 commun)
-- Helene (9) a demandé Testeur (10)

-- 8. NOTATIONS (Inchangé pour l'instant - Supposant que tous peuvent noter les events auxquels ils ont participé)
INSERT INTO notations (event_id, membre_id, ambiance, proprete, organisation, fair_play, niveau_joueurs, date_notation)
VALUES (1, 1, 5, 4, 5, 5, 4, '2025-02-16 10:00:00'),   -- Alice (ADMIN) note Event 1
       (1, 3, 4, 3, 4, 4, 3, '2025-02-16 11:00:00'),   -- Bob (MEMBRE) note Event 1
       (2, 1, 5, 5, 4, 5, 3, '2025-03-21 10:00:00'),   -- Alice (ADMIN) note Event 2
       (2, 3, 4, 4, 3, 4, 3, '2025-03-21 11:00:00'),   -- Bob (MEMBRE) note Event 2
       (7, 5, 5, 4, 4, 5, 4, '2025-03-02 09:00:00'),   -- Eva (MEMBRE) note Event 7
       (7, 6, 4, 5, 5, 4, 3, '2025-03-02 10:00:00'),
       -- Nouvelles notations par des MEMBRES pour des événements PASSÉS (1, 2, 10, 13)
       (1, 4, 5, 4, 5, 5, 4, '2025-02-17 09:00:00'),   -- David (MEMBRE) note Event 1 (Badminton)
       (10, 13, 3, 5, 4, 5, 2, '2025-04-13 10:00:00'), -- Isabelle (MEMBRE) note Event 10 (Nettoyage)
       (10, 15, 4, 5, 5, 5, 1, '2025-04-13 11:00:00'), -- Karim (MEMBRE) note Event 10 (Nettoyage)
       (13, 3, 5, 4, 4, 5, 3, '2025-04-02 10:00:00'),  -- Bob (MEMBRE) note Event 13 (Apéro)
       (13, 12, 4, 3, 3, 4, 2, '2025-04-02 11:00:00');
-- Hélène (MEMBRE) note Event 13 (Apéro)
-- Franck (ADMIN) note Event 7

-- ============================================================
-- == FIN DES DONNÉES DE TEST (NETTOYÉES ET COHÉRENTES)      ==
-- ============================================================
