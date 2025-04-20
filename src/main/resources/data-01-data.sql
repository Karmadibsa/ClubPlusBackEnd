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
        'MEMBRE', 1, NULL);

-- 2. CLUBS (Inchangé)
INSERT INTO club (id, nom, date_creation, date_inscription, numero_voie, rue, codepostal, ville, telephone, email,
                  actif, desactivation_date, code_club)
VALUES (1, 'Club Omnisports Paris Centre', '2010-01-15', '2024-01-10', '1', 'Place du Sport', '75001', 'Paris',
        '0102030405', 'contact@csc-paris.fr', 1, NULL, 'CLUB-0001'),
       (2, 'Lyon Padel Club', '2018-06-01', '2025-01-01', '200', 'Avenue Padel', '69007', 'Lyon', '0477889900',
        'contact@lyonpadel.fr', 1, NULL, 'CLUB-0002'),
       (3, '[Désactivé] Ancien Club Test', '2005-05-05', '2022-01-01', '12', 'Rue Oubliée', '12345', 'TestVille',
        '0000000000', 'inactive+3@clubplus.invalid', 0, '2024-11-15 10:00:00', 'CLUB-0003'),
       (4, 'Club Sportif Bordeaux Ouest', '2019-09-01', '2025-03-01', '50', 'Avenue de la Marne', '33700', 'Merignac',
        '0556565656', 'contact@csbo.fr', 1, NULL, 'CLUB-0004'),
       (5, 'Club Pour Suppression', '2021-01-01', '2025-04-10', '1', 'Impasse Test', '99999', 'DeleteVille',
        '0707070707', 'delete@me.com', 1, NULL, 'CLUB-0005');

-- 3. ADHESIONS (Inchangé - Associe utilisateurs aux clubs, rôles définis dans membre)
INSERT INTO adhesion (membre_id, club_id, date_adhesion)
VALUES (1, 1, '2024-01-10 10:00:00'), -- Alice (ADMIN) -> Club 1
       (2, 1, '2024-08-15 11:00:00'), -- Charlie (RESA) -> Club 1
       (3, 1, '2024-05-20 12:00:00'), -- Bob (MEMBRE) -> Club 1
       (4, 1, '2025-02-01 13:00:00'), -- David (MEMBRE) -> Club 1
       (5, 1, '2025-04-05 14:00:00'), -- Eva (MEMBRE) -> Club 1
       (5, 2, '2025-04-06 15:00:00'), -- Eva (MEMBRE) -> Club 2
       (6, 2, '2025-01-01 09:00:00'), -- Franck (ADMIN) -> Club 2
       (8, 4, '2025-03-01 10:00:00'), -- Gerard (ADMIN) -> Club 4
       (9, 4, '2025-03-05 11:00:00'), -- Helene (MEMBRE) -> Club 4
       (10, 1, '2025-04-01 12:00:00');
-- Testeur (MEMBRE) -> Club 1

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
       (8, 'Tournoi Volley Bordeaux (Futur)', '2025-07-05 09:00:00', '2025-07-05 18:00:00',
        'Tournoi estival sur sable.', 'Plage Océan', 4, 1, NULL),
       (9, 'Réunion Info Suppression (Futur)', '2025-05-25 19:00:00', '2025-05-25 20:00:00', 'Présentation projet.',
        'Local Club 5', 5, 1, NULL),
       (10, 'Nettoyage Local CSC (Passé)', '2025-04-12 09:00:00', '2025-04-12 12:00:00',
        'Grand nettoyage de printemps.', 'Local Matériel', 1, 1, NULL);

-- 5. CATEGORIES (Capacité Cat 5 ajustée pour Test 53)
INSERT INTO categories (id, event_id, nom, capacite)
VALUES (1, 1, 'Simple H', 16),
       (2, 1, 'Simple F', 16),
       (3, 2, 'Buffet', 50),
       (4, 3, 'P100 Hommes', 24),
       (5, 4, 'Débutant', 2), -- Capacité 2 pour Test 53
       (6, 5, 'Stage Intensif', 10),
       (7, 4, 'Confirmés', 8),
       (8, 4, 'Avancés', 6),
       (9, 2, 'Boissons', 60),
       (10, 6, 'Jeux de Société', 20),
       (11, 7, 'Initiation Padel', 15),
       (12, 8, 'Tournoi Mixte 4x4', 8),
       (13, 8, 'Tournoi Masculin 4x4', 8),
       (14, 9, 'Présentation', 30),
       (15, 10, 'Equipe Nettoyage', 10);

-- 6. RESERVATIONS (NETTOYÉES : Uniquement MEMBREs)
INSERT INTO reservations (membre_id, event_id, categorie_id, date_reservation, reservation_uuid, status)
VALUES -- Réservations pour MEMBREs uniquement
       (3, 1, 2, '2025-02-02 11:00:00', UUID(), 'UTILISE'),   -- Bob @ Badminton (Passé)
       (3, 2, 3, '2025-03-01 11:00:00', UUID(), 'UTILISE'),   -- Bob @ Soirée Mars (Passé)
       (5, 3, 4, '2025-05-01 09:00:00', UUID(), 'CONFIRME'),  -- Eva @ Padel Lyon (Futur) - Retrait doublon
       (4, 4, 5, '2025-04-21 15:00:00', UUID(), 'ANNULE'),    -- David @ Escalade (Annulée)
       (5, 5, 6, '2025-04-10 10:00:00', UUID(), 'ANNULE'),    -- Eva @ Stage Annulé
       (5, 7, 11, '2025-02-25 10:00:00', UUID(), 'UTILISE'),  -- Eva @ Stage Padel Lyon (Passé)
       (9, 8, 12, '2025-06-11 10:00:00', UUID(), 'CONFIRME'), -- Helene @ Volley Bordeaux (Futur)
       -- Ajout pour Test Capacité Pleine (Test 53)
       (3, 4, 5, '2025-04-20 15:00:00', UUID(), 'CONFIRME'),  -- Bob @ Escalade (Cat 5, Capa 2)
       (5, 4, 5, '2025-04-20 15:01:00', UUID(), 'CONFIRME'),
       (4, 1, 1, '2025-02-01 10:00:00', UUID(), 'UTILISE');
-- David (4) a participé à Event 1 / Cat 1;
-- Eva @ Escalade (Cat 5, Capa 2)

-- 7. DEMANDES D'AMIS (NETTOYÉES : Uniquement entre MEMBREs)
INSERT INTO demande_ami (envoyeur_id, recepteur_id, statut, date_demande)
VALUES -- Interactions entre MEMBREs uniquement
       (3, 4, 'ACCEPTE', '2025-03-15 13:00:00'), -- Bob (3) et David (4) sont amis
       (5, 3, 'REFUSE', '2025-04-10 14:00:00'),  -- Eva (5) a refusé Bob (3)
       (4, 5, 'ATTENTE', '2025-04-18 10:00:00'), -- David (4) a demandé Eva (5)
       (9, 10, 'ATTENTE', '2025-04-19 11:00:00'),
       (5, 10, 'ATTENTE', '2025-04-20 17:00:00');;
-- Helene (9) a demandé Testeur (10)

-- 8. NOTATIONS (Inchangé pour l'instant - Supposant que tous peuvent noter les events auxquels ils ont participé)
INSERT INTO notations (event_id, membre_id, ambiance, proprete, organisation, fair_play, niveau_joueurs, date_notation)
VALUES (1, 1, 5, 4, 5, 5, 4, '2025-02-16 10:00:00'), -- Alice (ADMIN) note Event 1
       (1, 3, 4, 3, 4, 4, 3, '2025-02-16 11:00:00'), -- Bob (MEMBRE) note Event 1
       (2, 1, 5, 5, 4, 5, 3, '2025-03-21 10:00:00'), -- Alice (ADMIN) note Event 2
       (2, 3, 4, 4, 3, 4, 3, '2025-03-21 11:00:00'), -- Bob (MEMBRE) note Event 2
       (7, 5, 5, 4, 4, 5, 4, '2025-03-02 09:00:00'), -- Eva (MEMBRE) note Event 7
       (7, 6, 4, 5, 5, 4, 3, '2025-03-02 10:00:00');
-- Franck (ADMIN) note Event 7

-- ============================================================
-- == FIN DES DONNÉES DE TEST (NETTOYÉES ET COHÉRENTES)      ==
-- ============================================================
