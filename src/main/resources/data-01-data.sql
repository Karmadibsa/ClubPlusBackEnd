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
INSERT INTO membre (id, nom, prenom, date_naissance, date_inscription, telephone,
                    email, password, role, code_ami, actif, anonymize_date, verified) -- Ajout de code_ami ici
VALUES (1, 'AdminClub', 'Alice', '1988-03-10', '2024-01-10', '0601010101',
        'alice.admin@club.fr', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG', 'ADMIN', 'AMIS-000001',
        1, NULL, 1),                -- Ajout valeur
       (2, 'ResaClub', 'Charlie', '1992-07-01', '2024-08-15', '0603030303',
        'charlie.resa@club.fr', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG', 'RESERVATION',
        'AMIS-000002', 1, NULL, 1), -- Ajout valeur
       (3, 'MembreSimple', 'Bob', '1995-11-25', '2024-05-20', '0602020202',
        'bob.membre@email.com', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG', 'MEMBRE', 'AMIS-000003',
        1, NULL, 1),                -- Ajout valeur
       (4, 'Sargent', 'Menault', '1985-09-29', '2013-08-06', '687 690 2805',
        'smenault3@chronoengine.com', '$2a$04$cyudbCMPNE8jlTgquVYw/e2b8oU5YQw6tRn00/h.U3G9ceGc3WWzO', 'MEMBRE',
        'AMIS-000004', 1, NULL, 1),
       (5, 'Johnny', 'Gillivrie', '2006-04-08', '2018-03-24', '915 104 6224',
        'momper.axel.99@gmail.com', '$2y$10$A7.AsjGP0ptMeaRHIivES.8YyMXBSuCYy0T6F6.7Id1Ih5p/3hihG', 'MEMBRE',
        'AMIS-000005',
        1, NULL, 1),
       (6, 'Ethelda', 'Bartak', '2005-01-16', '2019-09-04', '418 786 9932',
        'ebartak5@bizjournals.com', '$2a$04$Q91VSCpTvYeaS/FPv8ElkOm1F340foJE/lrk6a9GN2eBL.fDxK7Ba', 'MEMBRE',
        'AMIS-000006', 1, NULL, 1),
       (7, 'Arnoldo', 'Chattell', '1959-05-27', '2010-10-18', '978 865 1680',
        'achattell6@mysql.com', '$2a$04$xnUk7k9xi.lfWU3BobPKk.DTztzLBdUV5TRc6h/6xC2QyBBp2ldv.', 'MEMBRE', 'AMIS-000007',
        1, NULL, 1),
       (8, 'Goran', 'Findon', '1956-09-16', '2025-01-11', '896 435 1983',
        'gfindon7@wiley.com', '$2a$04$J4lbR3b8y9BYUcc5mS5uq.u75UXTfeDeHCE0TqXRn8Lkf06tfMWQe', 'MEMBRE', 'AMIS-000008',
        1, NULL, 1),
       (9, 'Christoph', 'Cantor', '2004-12-12', '2015-05-19', '670 221 3257',
        'ccantor8@zimbio.com', '$2a$04$bCEUB1Dcb2nkcPO9ASyMzeCipdq.0Qg3.ZpPyDIOQ/wjbe11fXCOq', 'MEMBRE', 'AMIS-000009',
        1, NULL, 1),
       (10, 'Perceval', 'Fedorchenko', '1983-06-29', '2017-02-22',
        '677 418 4812', 'pfedorchenko9@reverbnation.com',
        '$2a$04$xgnaOziWf6DBAaZL5NPzFOzEDhqOH2IInrKgcd1dbQ7Xa/ckA.I96', 'MEMBRE', 'AMIS-000010', 1, NULL, 1),
       (11, 'Maryl', 'Wheeliker', '1981-04-12', '2014-08-25', '748 464 9282',
        'mwheelikera@last.fm', '$2a$04$46k50JtFcpxxq0tptg0/Iuy7ZkArO.hHLh6REOfL.DfQigpDkNqIW', 'MEMBRE', 'AMIS-000011',
        1, NULL, 1),
       (12, 'Lorene', 'Pickthorn', '1975-05-18', '2024-03-02', '531 278 2701',
        'lpickthornb@cloudflare.com', '$2a$04$QdnzALGHRTN4hpVlkhsb3.mvt3hzvNfvxrDsSVJNdCOL3hZfLK2GS', 'MEMBRE',
        'AMIS-000012', 1, NULL, 1),
       (13, 'Lilias', 'Pirdue', '1976-08-22', '2014-11-17', '756 995 8034',
        'lpirduec@hubpages.com', '$2a$04$r0fGsgHRb9dnWItityovNOuxpUt8JOiXS09p5SmGuCuzLmSHWJ4BC', 'MEMBRE',
        'AMIS-000013', 1, NULL, 1),
       (14, 'Winslow', 'Dorber', '1971-08-15', '2010-01-08', '425 995 1464',
        'wdorberd@go.com', '$2a$04$seJv1KmeEKgLRQg6lif1jusF7.utAdZr8sHaVgewvYBzdql2Lure6', 'MEMBRE', 'AMIS-000014', 1,
        NULL, 1),
       (15, 'Mary', 'Jeanon', '1985-01-24', '2017-06-20', '424 451 7696',
        'mjeanone@yellowbook.com', '$2a$04$71JHV1RLwwd62sWK/XtrWOGT7sgV6BVziPgq6fpb2cMQoy68QEfky', 'MEMBRE',
        'AMIS-000015', 1, NULL, 1),
       (16, 'Jillene', 'Perutto', '1986-12-06', '2013-08-14', '925 956 0382',
        'jperuttof@a8.net', '$2a$04$aLT1y9BirTmWfJ9NJKHBk.XBmCswoo5.zPtc6Dut/OKbYHrqhwKxS', 'MEMBRE', 'AMIS-000016', 1,
        NULL, 1),
       (17, 'Kara', 'Goscomb', '2003-11-17', '2011-06-23', '408 200 4831',
        'kgoscombg@dmoz.org', '$2a$04$fB/UM5k0uBwH/JXF048M7eAbZm8vfukSFej7PCCpJmRGAWzt4mg9G', 'MEMBRE', 'AMIS-000017',
        1, NULL, 1),
       (18, 'Mortimer', 'Amos', '1972-11-24', '2015-02-14', '749 718 9986',
        'mamosh@census.gov', '$2a$04$FjFCGXhxIzDcesHFRIabaem0eSlG5kIwryybWd9Cxi4jHdVF45Jk6', 'MEMBRE', 'AMIS-000018', 1,
        NULL, 1),
       (19, 'Dagmar', 'Buyers', '1953-10-11', '2010-03-12', '997 547 2790',
        'dbuyersi@multiply.com', '$2a$04$aSV2bQtNHlnlwQppPjvlgumeLAY4Ju.icE9HagEMwIJ4FT7yKxWlq', 'MEMBRE',
        'AMIS-000019', 1, NULL, 1),
       (20, 'Rafaello', 'Grane', '1989-03-31', '2013-04-03', '302 126 5279',
        'rgranej@shutterfly.com', '$2a$04$AkkT/mjWPuVvGIA84O8mQ.nYXalJVVf31q76SLV7mxBN7uSmycSa6', 'MEMBRE',
        'AMIS-000020', 1, NULL, 1),
       (21, 'Bron', 'Fern', '1968-10-22', '2011-12-21', '894 104 5309',
        'bfernk@yandex.ru', '$2a$04$2CFONdxclZyu0CrS6EZzxOOeQhQ.NBH2cGl5pULJ.eXxivMdp9W.W', 'MEMBRE', 'AMIS-000021', 1,
        NULL, 1),
       (22, 'Codee', 'Tayt', '1982-03-02', '2015-12-22', '401 496 8541',
        'ctaytl@google.cn', '$2a$04$DK7tOIyz6gYDWQHm4v/cruqwhZwW5zNzjtJ5WVSSDvOFblSDXpj0G', 'MEMBRE', 'AMIS-000022', 1,
        NULL, 1),
       (23, 'Tandie', 'Gligori', '1982-12-08', '2023-01-21', '842 197 9943',
        'tgligorim@usda.gov', '$2a$04$KANl9Pu.lnHeL7w85LBAcuQoQqZHH4q.qQro6W5q1S5yGlnN0kLmS', 'MEMBRE', 'AMIS-000023',
        1, NULL, 1),
       (24, 'Herminia', 'Gather', '1987-03-08', '2015-08-19', '924 839 2607',
        'hgathern@last.fm', '$2a$04$d.oBI/n4yH4G4vf6LwSFBenSq2A332eVt5XU/tEEDsmPlYHpF0oYC', 'MEMBRE', 'AMIS-000024', 1,
        NULL, 1),
       (25, 'Lisa', 'Ginnally', '1988-11-28', '2021-02-06', '840 983 8871',
        'lginnallyo@parallels.com', '$2a$04$l3/vvZ68BjOqbV2DKl233eQRkkzZtjZSBZtX2sm4aMom.Kl1i/uOO', 'MEMBRE',
        'AMIS-000025', 1, NULL, 1),
       (26, 'Skye', 'Hudless', '1989-12-25', '2020-01-08', '787 905 6247',
        'shudlessp@examiner.com', '$2a$04$u0StBLyb0jl7rFqtWCKjDeFnZ9JLI0R4F1T9dh7/dAeeTrpWkrgki', 'MEMBRE',
        'AMIS-000026', 1, NULL, 1),
       (27, 'Chalmers', 'Shopcott', '1994-07-19', '2024-05-28',
        '236 597 9294', 'cshopcottq@goo.ne.jp', '$2a$04$tLDeiBxn5okIa50L8LSWdOw6baTcqDah6WUp1wy2YZeNJv2aTzoIa',
        'MEMBRE', 'AMIS-000027', 1, NULL, 1),
       (28, 'Vincent', 'Dannell', '1986-07-06', '2021-06-19', '772 835 5417',
        'vdannellr@loc.gov', '$2a$04$Mo/XXFUiT/21jaqWYcs91uk5rfkPo4.zi9kt34G1FHoVNW64A/Gzy', 'MEMBRE', 'AMIS-000028', 1,
        NULL, 1),
       (29, 'Aidan', 'Gilfoy', '1963-09-26', '2014-02-01', '482 365 4525',
        'agilfoys@sun.com', '$2a$04$D4XWR/acmEbFwEiHwxUs/uKRD.1eT8HvtGz.MC27shFPqW/3YMp5y', 'MEMBRE', 'AMIS-000029', 1,
        NULL, 1),
       (30, 'Tomasine', 'Dyet', '1971-01-19', '2024-02-25', '947 843 2277',
        'tdyett@earthlink.net', '$2a$04$HD7h7zFkzDGu/MAwDCt.IOCk0GTz9rZjEAmNTwNMEsVodF5n1scEW', 'MEMBRE', 'AMIS-000030',
        1, NULL, 1);
-- Ajout valeur

-- 2. CLUBS (Inchangé)
INSERT INTO club (id, nom, date_creation, date_inscription, numero_voie, rue, codepostal, ville, telephone, email,
                  actif, desactivation_date, code_club)
VALUES (1, 'Club Omnisports Paris Centre', '2010-01-15', '2024-01-10', '1', 'Place du Sport', '75001', 'Paris',
        '0102030405', 'contact@csc-paris.fr', 1, NULL, 'CLUB-0001'),
       (2, 'Club Omnisports Metz Centre', '2010-01-15', '2024-01-10', '1', 'Place du Sport', '75001', 'Metz',
        '0102030405', 'contact@csc-metz.fr', 1, NULL, 'CLUB-0002');

-- 3. ADHESIONS (Inchangé - Associe utilisateurs aux clubs, rôles définis dans membre)
INSERT INTO adhesion (membre_id, club_id, date_adhesion)
VALUES (1, 1, '2025-04-06 18:26:39'),
       (2, 1, '2025-04-29 19:22:24'),
       (3, 1, '2025-02-21 06:31:37'),
       (4, 1, '2025-01-28 04:08:56'),
       (5, 1, '2025-04-27 14:46:49'),
       (6, 1, '2025-04-20 01:05:05'),
       (7, 1, '2025-01-09 03:41:54'),
       (8, 1, '2025-01-07 20:44:07'),
       (9, 1, '2025-02-16 03:08:09'),
       (10, 1, '2025-04-20 07:41:01'),
       (11, 1, '2025-01-30 12:28:45'),
       (12, 1, '2025-03-17 15:29:45'),
       (13, 1, '2025-04-28 08:44:12'),
       (14, 1, '2025-03-10 00:11:51'),
       (15, 1, '2025-02-20 00:57:17'),
       (16, 1, '2025-03-28 17:26:15'),
       (17, 1, '2025-01-20 13:36:17'),
       (18, 1, '2025-03-05 00:30:20'),
       (19, 1, '2025-04-05 11:14:09'),
       (20, 1, '2025-02-24 01:47:06'),
       (21, 1, '2025-04-27 18:56:26'),
       (22, 1, '2025-01-21 09:32:33'),
       (23, 1, '2025-02-15 13:15:59'),
       (24, 1, '2025-03-19 13:22:41'),
       (25, 1, '2025-03-27 22:34:13'),
       (26, 1, '2025-01-27 11:11:35'),
       (27, 1, '2025-04-10 13:14:44'),
       (28, 1, '2025-03-31 01:28:48'),
       (29, 1, '2025-02-06 02:33:17'),
       (30, 1, '2025-03-20 06:47:32');
-- Karim -> Club 1

-- 4. EVENTS (Dates Event 4 ajustées pour être en cours au 20 Avril)
INSERT INTO events (id, nom, start_time, end_time, description, location, organisateur_id, actif, desactivation_date)
VALUES (1, 'Tournoi Badminton CSC (Passé)', '2025-02-15 14:00:00', '2025-02-15 18:00:00', 'Tournoi amical interne.',
        'Gymnase Sud', 1, 1, NULL),
       (2, 'Soirée CSC Mars (Passé)', '2025-03-20 19:00:00', '2025-03-20 23:00:00', 'Rencontre membres.', 'Club House',
        1, 1, NULL),
       (3, 'Tournoi Padel Lyon (Futur)', '2025-06-07 10:00:00', '2025-06-07 17:00:00', 'Tournoi P100 Hommes.',
        'Lyon Padel Est', 1, 1, NULL),
       (4, 'Initiation Escalade CSC (En Cours)', NOW() + INTERVAL 1 HOUR, NOW() + INTERVAL 3 HOUR, 'Découverte salle.',
        'Salle GrimpUp', 1, 1, NULL),
       (5, '[Annulé] Stage Tennis CSC (Futur)', '2025-07-10 09:00:00', '2025-07-12 17:00:00', 'Stage intensif.',
        'Courts Central', 1, 0, '2025-04-15 09:30:00'),
       (6, 'Soirée Jeux Club Paris (Futur)', '2025-08-10 19:00:00', '2025-08-10 23:00:00', 'Rencontre amicale jeux.',
        'Salle Polyvalente', 1, 1, NULL),
       (7, 'Stage Padel Découverte Lyon (Passé)', '2025-03-01 10:00:00', '2025-03-01 14:00:00',
        'Initiation et découverte.', 'Lyon Padel Ouest', 1, 1, NULL),
       -- AJOUTER EVENT 8 --
       (8, 'Conférence Tech CSC (Futur)', '2025-10-15 18:00:00', '2025-10-15 21:00:00',
        'Conférence sur les nouvelles technos.', 'Amphithéâtre', 1, 1, NULL),
       -- AJOUTER EVENT 9 --
       (9, 'Match Foot Amical CSC (Futur)', '2025-05-25 15:00:00', '2025-05-25 17:00:00', 'Match amical interne.',
        'Terrain Principal', 1, 1, NULL),
       (10, 'Nettoyage Local CSC (Passé)', '2025-04-12 09:00:00', '2025-04-12 12:00:00',
        'Grand nettoyage de printemps.', 'Local Matériel', 1, 1, NULL),
       (11, 'Stage Yoga Découverte CSC (Futur)', '2025-05-10 10:00:00', '2025-05-10 12:00:00',
        'Initiation aux postures de base.', 'Salle Zen', 1, 1, NULL),
       (12, 'Tournoi Tennis Double Mixte CSC (Futur)', '2025-09-14 09:00:00', '2025-09-14 17:00:00',
        'Tournoi amical ouvert à tous les niveaux.', 'Courts Central', 1, 1, NULL),
       (13, 'Apéro Afterwork CSC (Passé)', '2025-04-01 18:30:00', '2025-04-01 21:00:00',
        'Moment convivial après le travail.', 'Bar du Club', 1, 1, NULL),
       (14, 'sit', '2025-10-22 20:23:46', '2025-10-22 21:23:46',
        'sem sed sagittis nam congue risus semper porta volutpat quam pede lobortis ligula sit amet eleifend pede libero',
        'Gymnase', 1, 1, NULL),
       (15, 'tristique in', '2025-06-25 22:30:02', '2025-06-25 23:30:02',
        'nibh in hac habitasse platea dictumst aliquam augue quam sollicitudin vitae consectetuer eget rutrum at lorem integer tincidunt ante',
        'Gymnase', 1, 1, NULL),
       (16, 'orci', '2025-02-22 18:14:12', '2025-02-22 19:14:12',
        'diam erat fermentum justo nec condimentum neque sapien placerat ante nulla justo aliquam quis turpis eget elit',
        'Salle Polyvalente', 1, 1, NULL),
       (17, 'eget rutrum at', '2025-05-06 21:59:34', '2025-05-06 22:59:34',
        'porta volutpat erat quisque erat eros viverra eget congue eget semper rutrum nulla nunc purus phasellus',
        'Parking', 1, 1, NULL),
       (18, 'elementum pellentesque', '2025-11-19 04:53:31', '2025-11-19 05:53:31',
        'duis consequat dui nec nisi volutpat eleifend donec ut dolor morbi vel', 'Gymnase', 1, 0,
        '2025-05-02 05:51:12'),
       (19, 'vel accumsan', '2025-06-22 19:28:17', '2025-06-22 20:28:17',
        'nec sem duis aliquam convallis nunc proin at turpis a pede', 'Salle Polyvalente', 1, 0, '2025-05-02 05:51:12'),
       (20, 'integer', '2025-03-07 06:57:56', '2025-03-07 07:57:56',
        'lacus at velit vivamus vel nulla eget eros elementum pellentesque', 'Gymnase Sud', 1, 1, NULL),
       (21, 'morbi ut', '2025-07-14 17:17:13', '2025-07-14 18:17:13',
        'magna ac consequat metus sapien ut nunc vestibulum ante ipsum primis in faucibus orci luctus et',
        'Champ de blé', 1, 0, '2025-05-02 05:51:12'),
       (22, 'eu interdum eu', '2025-05-22 22:40:37', '2025-05-22 23:40:37',
        'in consequat ut nulla sed accumsan felis ut at dolor quis', 'Gymnase', 1, 1, NULL),
       (23, 'nec dui luctus', '2025-04-02 04:46:38', '2025-04-02 05:46:38',
        'in purus eu magna vulputate luctus cum sociis natoque penatibus et magnis dis parturient montes',
        'Terrain Principal', 1, 0, '2025-05-02 05:51:12'),
       (24, 'diam', '2025-08-29 21:53:04', '2025-08-29 22:53:04',
        'dignissim vestibulum vestibulum ante ipsum primis in faucibus orci luctus et', 'Gymnase Sud', 1, 1, NULL),
       (25, 'nulla', '2025-02-09 08:33:11', '2025-02-09 09:33:11',
        'mauris morbi non lectus aliquam sit amet diam in magna bibendum', 'Terrain Principal', 1, 0,
        '2025-05-02 05:51:12'),
       (26, 'eleifend donec ut', '2025-04-24 00:55:16', '2025-04-24 01:55:16',
        'mi pede malesuada in imperdiet et commodo vulputate justo in blandit ultrices enim lorem ipsum dolor sit amet consectetuer adipiscing',
        'Salle Polyvalente', 1, 1, NULL),
       (27, 'viverra eget congue', '2025-05-20 14:10:43', '2025-05-20 15:10:43',
        'cubilia curae duis faucibus accumsan odio curabitur convallis duis consequat dui nec nisi volutpat eleifend',
        'Terrain Principal', 1, 1, NULL),
       (28, 'maecenas tristique est', '2025-08-18 21:45:37', '2025-08-18 22:45:37',
        'ut massa volutpat convallis morbi odio odio elementum eu interdum eu tincidunt in', 'Gymnase', 1, 1, NULL),
       (29, 'dictumst aliquam', '2025-06-14 03:54:32', '2025-06-14 04:54:32',
        'vel nisl duis ac nibh fusce lacus purus aliquet at feugiat non pretium quis lectus suspendisse potenti in eleifend quam',
        'Terrain Principal', 1, 1, NULL),
       (30, 'molestie lorem', '2025-10-12 12:02:48', '2025-10-12 13:02:48',
        'libero quis orci nullam molestie nibh in lectus pellentesque at', 'Gymnase', 1, 1, null);

-- 5. CATEGORIES (Capacité Cat 5 ajustée pour Test 53)
-- INSERT statements for 'categories' table for 30 events

INSERT INTO categories (id, event_id, nom, capacite)
VALUES
    -- Event 1 (Badminton)
    (1, 1, 'Simple H Compétition', 16),
    (2, 1, 'Simple F Compétition', 16),
    (3, 1, 'Spectateurs', 50),
    -- Event 2 (Soirée)
    (4, 2, 'Participants Repas', 60),
    (5, 2, 'Animation Musicale', 100),
    -- Event 3 (Padel Tournoi)
    (6, 3, 'P100 Hommes', 24),
    (7, 3, 'P100 Femmes', 16),
    -- Event 4 (Escalade Initiation)
    (8, 4, 'Débutant Adulte', 12),
    (9, 4, 'Enfants 8-12 ans', 10),
    -- Event 5 (Stage Judo)
    (10, 5, 'Stage Ceinture Blanche/Jaune', 10),
    (11, 5, 'Stage Ceinture Orange/Verte', 8),
    -- Event 6 (Tournoi Échecs)
    (12, 6, 'Tournoi Principal', 32),
    -- Event 7 (Initiation Padel)
    (13, 7, 'Groupe Initiation', 15),
    -- Event 8 (Conférence Tech)
    (14, 8, 'Participants Conférence', 75),
    -- Event 9 (Match Foot Amical)
    (15, 9, 'Joueurs Équipe A', 11),
    (16, 9, 'Joueurs Équipe B', 11),
    (17, 9, 'Supporters', 100),
    -- Event 10 (Nettoyage Parc)
    (18, 10, 'Bénévoles Nettoyage', 20),
    -- Event 11 (Yoga Doux)
    (19, 11, 'Participants Yoga', 15),
    -- Event 12 (Tournoi Tennis Mixte)
    (20, 12, 'Équipes Double Mixte', 12),   -- 12 équipes = 24 joueurs
    -- Event 13 (Apéro Afterwork)
    (21, 13, 'Participants Apéro', 40),
    -- Event 14 (Atelier Cuisine)
    (22, 14, 'Participants Atelier', 12),
    -- Event 15 (Randonnée)
    (23, 15, 'Randonneurs Confirmés', 25),
    -- Event 16 (Tournoi Volley)
    (24, 16, 'Équipes 4x4 Loisir', 8),      -- 8 équipes = 32 joueurs
    (25, 16, 'Arbitres/Organisation', 4),
    -- Event 17 (Ciné Club)
    (26, 17, 'Spectateurs Film', 40),
    -- Event 18 (Séminaire Marketing)
    (27, 18, 'Participants Séminaire', 30),
    -- Event 19 (Compétition Natation)
    (28, 19, 'Nageurs 100m NL', 16),
    (29, 19, 'Équipes Relais 4x50m', 8),
    -- Event 20 (Soirée Jeux)
    (30, 20, 'Joueurs Jeux de Société', 25),
    -- Event 21 (Formation Secourisme)
    (31, 21, 'Stagiaires PSC1', 12),
    -- Event 22 (Course Orientation)
    (32, 22, 'Équipes Famille', 15),
    (33, 22, 'Équipes Adultes', 10),
    -- Event 23 (Assemblée Générale)
    (34, 23, 'Membres Votants', 80),
    -- Event 24 (Workshop Photo)
    (35, 24, 'Participants Workshop', 10),
    -- Event 25 (Pétanque Loisir)
    (36, 25, 'Doublettes Formées', 24),     -- 24 équipes = 48 joueurs
    -- Event 26 (Cours de Danse)
    (37, 26, 'Couples Salsa Débutant', 10), -- 10 couples = 20 personnes
    (38, 26, 'Couples Salsa Intermédiaire', 8),
    -- Event 27 (Sortie VTT)
    (39, 27, 'Groupe VTT Bleu', 18),
    -- Event 28 (Concours Pêche)
    (40, 28, 'Pêcheurs Individuels', 30),
    -- Event 29 (Théâtre Impro)
    (41, 29, 'Comédiens Impro', 14),
    (42, 29, 'Public Théâtre', 40),
    -- Event 30 (Pot Départ)
    (43, 30, 'Invités Pot', 25),
    -- Catégories supplémentaires pour diversité
    (44, 7, 'Observateurs Padel', 10),
    (45, 14, 'Dégustation Cuisine', 12);

-- 6. RESERVATIONS (NETTOYÉES : Uniquement MEMBREs)
INSERT INTO reservations (membre_id, event_id, categorie_id, date_reservation, reservation_uuid, status)
VALUES -- Réservations pour MEMBREs uniquement
       -- Insérez ces lignes après votre `INSERT INTO reservations (...) VALUES`
       (4, 1, 1, '2025-02-01 09:00:00', UUID(), 'UTILISE'),     -- Membre 4 @ Event 1 (Passé) / Cat 1
       (3, 1, 1, '2025-02-01 09:00:00', UUID(), 'UTILISE'),     -- Membre 4 @ Event 1 (Passé) / Cat 1
       (5, 2, 4, '2025-03-10 10:30:00', UUID(), 'UTILISE'),     -- Membre 5 @ Event 2 (Passé) / Cat 4
       (3, 2, 4, '2025-03-10 10:30:00', UUID(), 'UTILISE'),     -- Membre 5 @ Event 2 (Passé) / Cat 4
       (6, 3, 6, '2025-05-15 11:00:00', UUID(), 'CONFIRME'),    -- Membre 6 @ Event 3 (Futur) / Cat 6
       (5, 3, 6, '2025-05-15 11:00:00', UUID(), 'CONFIRME'),    -- Membre 6 @ Event 3 (Futur) / Cat 6
       (5, 4, 8, '2025-05-15 11:00:00', UUID(), 'CONFIRME'),    -- Membre 6 @ Event 3 (Futur) / Cat 6
       (7, 4, 8, '2025-04-18 14:00:00', UUID(), 'UTILISE'),     -- Membre 7 @ Event 4 (Passé récent) / Cat 8
       (3, 4, 8, '2025-04-18 14:00:00', UUID(), 'CONFIRME'),    -- Membre 7 @ Event 4 (Passé récent) / Cat 8
       (8, 7, 13, '2025-02-20 15:00:00', UUID(), 'UTILISE'),    -- Membre 8 @ Event 7 (Passé) / Cat 13
       (9, 9, 15, '2025-05-20 16:00:00', UUID(), 'CONFIRME'),   -- Membre 9 @ Event 9 (Futur) / Cat 15 (joueur A)
       (5, 9, 15, '2025-05-20 16:00:00', UUID(), 'CONFIRME'),   -- Membre 9 @ Event 9 (Futur) / Cat 15 (joueur A)
       (10, 9, 16, '2025-05-21 17:00:00', UUID(), 'CONFIRME'),  -- Membre 10 @ Event 9 (Futur) / Cat 16 (joueur B)
       (11, 10, 18, '2025-04-05 09:30:00', UUID(), 'UTILISE'),  -- Membre 11 @ Event 10 (Passé) / Cat 18
       (12, 11, 19, '2025-04-30 10:00:00', UUID(), 'CONFIRME'), -- Membre 12 @ Event 11 (Futur) / Cat 19
       (5, 11, 19, '2025-04-30 10:00:00', UUID(), 'CONFIRME'),  -- Membre 12 @ Event 11 (Futur) / Cat 19
       (13, 12, 20, '2025-07-01 11:00:00', UUID(), 'CONFIRME'), -- Membre 13 @ Event 12 (Futur) / Cat 20
       (14, 13, 21, '2025-03-25 18:00:00', UUID(), 'UTILISE'),  -- Membre 14 @ Event 13 (Passé) / Cat 21
       (15, 14, 22, '2025-09-01 12:00:00', UUID(), 'CONFIRME'), -- Membre 15 @ Event 14 (Futur) / Cat 22
       (16, 15, 23, '2025-06-10 13:00:00', UUID(), 'CONFIRME'), -- Membre 16 @ Event 15 (Futur) / Cat 23
       (5, 15, 23, '2025-06-10 13:00:00', UUID(), 'CONFIRME'),  -- Membre 16 @ Event 15 (Futur) / Cat 23
       (17, 16, 24, '2025-02-15 14:00:00', UUID(), 'UTILISE'),  -- Membre 17 @ Event 16 (Passé) / Cat 24
       (18, 17, 26, '2025-04-20 15:00:00', UUID(), 'CONFIRME'), -- Membre 18 @ Event 17 (Futur) / Cat 26
       (19, 19, 28, '2025-05-05 16:00:00', UUID(), 'ANNULE'),   -- Membre 19 @ Event 19 (Futur, inactif) / Cat 28 - Annulé
       (20, 20, 30, '2025-03-01 17:00:00', UUID(), 'UTILISE'),  -- Membre 20 @ Event 20 (Passé) / Cat 30
       (23, 23, 34, '2025-08-15 11:00:00', UUID(), 'CONFIRME'), -- Membre 23 @ Event 23 (Futur) / Cat 34
       (24, 24, 35, '2025-09-05 12:00:00', UUID(), 'CONFIRME'), -- Membre 24 @ Event 24 (Futur) / Cat 35
       (25, 25, 36, '2025-10-01 13:00:00', UUID(), 'CONFIRME'), -- Membre 25 @ Event 25 (Futur) / Cat 36
       (26, 26, 37, '2025-11-10 14:00:00', UUID(), 'CONFIRME'), -- Membre 26 @ Event 26 (Futur) / Cat 37
       (27, 27, 39, '2025-11-20 15:00:00', UUID(), 'CONFIRME'), -- Membre 27 @ Event 27 (Futur) / Cat 39
       (28, 28, 40, '2025-12-01 16:00:00', UUID(), 'CONFIRME'), -- Membre 28 @ Event 28 (Futur) / Cat 40
       (29, 29, 41, '2025-12-10 17:00:00', UUID(), 'CONFIRME'), -- Membre 29 @ Event 29 (Futur) / Cat 41
       (30, 30, 43, '2025-12-15 18:00:00', UUID(), 'CONFIRME');
-- Membre 30 @ Event 30 (Futur) / Cat 43

-- Hélène @ Apéro
-- David (4) a participé à Event 1 / Cat 1;
-- Eva @ Escalade (Cat 5, Capa 2)

-- 7. DEMANDES D'AMIS (NETTOYÉES : Uniquement entre MEMBREs)
INSERT INTO demande_ami (envoyeur_id, recepteur_id, statut, date_demande)
VALUES -- Interactions entre MEMBREs uniquement
       -- Ajoutez ces lignes après vos `VALUES` existants pour la table `demande_ami`

       -- Demandes impliquant Bob (ID 3)
       (3, 5, 'ATTENTE', '2025-04-28 10:00:00'),   -- Bob (3) redemande Eva (5) (précédente refusée)
       (3, 6, 'ATTENTE', '2025-04-29 11:00:00'),   -- Bob (3) demande Ethelda (6)
       (3, 7, 'ATTENTE', '2025-04-30 12:00:00'),   -- Bob (3) demande Arnoldo (7)
       (3, 8, 'ACCEPTE', '2025-04-15 09:00:00'),   -- Bob (3) avait demandé Goran (8) (Accepté)
       (3, 9, 'REFUSE', '2025-04-16 10:00:00'),    -- Bob (3) avait demandé Christoph (9) (Refusé)
       (3, 10, 'ACCEPTE', '2025-04-17 11:00:00'),  -- Bob (3) avait demandé Perceval (10) (Accepté)
       -- Note: (3, 11, ...) est déjà dans votre liste en ATTENTE, on peut le laisser ou le modifier plus tard
       (3, 12, 'ATTENTE', '2025-05-01 08:00:00'),  -- Bob (3) demande Lorene (12)
       (3, 14, 'ATTENTE', '2025-05-01 09:00:00'),  -- Bob (3) demande Winslow (14)
       (3, 16, 'ATTENTE', '2025-05-01 10:00:00'),  -- Bob (3) demande Jillene (16)
       (3, 20, 'ATTENTE', '2025-05-01 11:00:00'),  -- Bob (3) demande Rafaello (20)
       (3, 25, 'ATTENTE', '2025-05-01 12:00:00'),  -- Bob (3) demande Lisa (25)
       (18, 3, 'ATTENTE', '2025-05-02 09:00:00'),  -- Mortimer (18) demande Bob (3)
       (21, 3, 'ACCEPTE', '2025-04-20 10:00:00'),  -- Bron (21) avait demandé Bob (3) (Accepté)
       (28, 3, 'ATTENTE', '2025-05-02 11:00:00'),  -- Vincent (28) demande Bob (3)
       (30, 3, 'REFUSE', '2025-04-22 13:00:00'),   -- Tomasine (30) avait demandé Bob (3) (Refusé)

       -- Autres demandes variées (IDs 4-30)
       (4, 6, 'ACCEPTE', '2025-03-20 10:00:00'),   -- David (4) et Ethelda (6) sont amis
       (4, 7, 'ATTENTE', '2025-04-30 14:00:00'),   -- David (4) demande Arnoldo (7)
       (5, 6, 'REFUSE', '2025-04-01 09:00:00'),    -- Eva (5) a refusé Ethelda (6)
       (5, 8, 'ATTENTE', '2025-05-01 13:00:00'),   -- Eva (5) demande Goran (8)
       (6, 9, 'ATTENTE', '2025-04-28 15:00:00'),   -- Ethelda (6) demande Christoph (9)
       (7, 10, 'ACCEPTE', '2025-04-05 16:00:00'),  -- Arnoldo (7) et Perceval (10) sont amis
       (8, 11, 'ATTENTE', '2025-04-29 17:00:00'),  -- Goran (8) demande Maryl (11)
       (9, 12, 'REFUSE', '2025-04-11 18:00:00'),   -- Christoph (9) a refusé Lorene (12)
       (10, 13, 'ATTENTE', '2025-04-30 19:00:00'), -- Perceval (10) demande Lilias (13)
       (11, 14, 'ACCEPTE', '2025-04-15 20:00:00'), -- Maryl (11) et Winslow (14) sont amis
       (12, 15, 'ATTENTE', '2025-05-01 21:00:00'), -- Lorene (12) demande Mary (15)
       (13, 16, 'REFUSE', '2025-04-18 08:00:00'),  -- Lilias (13) a refusé Jillene (16)
       (14, 17, 'ATTENTE', '2025-05-02 09:30:00'), -- Winslow (14) demande Kara (17)
       (15, 18, 'ACCEPTE', '2025-04-22 10:30:00'), -- Mary (15) et Mortimer (18) sont amis
       (16, 19, 'ATTENTE', '2025-05-02 11:30:00'), -- Jillene (16) demande Dagmar (19)
       (17, 20, 'REFUSE', '2025-04-25 12:30:00'),  -- Kara (17) a refusé Rafaello (20)
       (19, 21, 'ATTENTE', '2025-05-02 13:30:00'), -- Dagmar (19) demande Bron (21)
       (20, 22, 'ACCEPTE', '2025-04-28 14:30:00'), -- Rafaello (20) et Codee (22) sont amis
       (22, 23, 'ATTENTE', '2025-05-02 15:30:00'), -- Codee (22) demande Tandie (23)
       (23, 24, 'REFUSE', '2025-04-30 16:30:00'),  -- Tandie (23) a refusé Herminia (24)
       (24, 25, 'ATTENTE', '2025-05-02 17:30:00'), -- Herminia (24) demande Lisa (25)
       (25, 26, 'ACCEPTE', '2025-05-01 18:30:00'), -- Lisa (25) et Skye (26) sont amis
       (26, 27, 'ATTENTE', '2025-05-02 19:30:00'), -- Skye (26) demande Chalmers (27)
       (27, 29, 'REFUSE', '2025-05-01 20:30:00'),  -- Chalmers (27) a refusé Aidan (29)
       (29, 30, 'ATTENTE', '2025-05-02 21:30:00');
-- Aidan (29) demande Tomasine (30)

-- Eva (5) demande Isabelle (13) (Club 1 ou 2, ici Club 1 commun)
-- Helene (9) a demandé Testeur (10)

-- 8. NOTATIONS (Inchangé pour l'instant - Supposant que tous peuvent noter les events auxquels ils ont participé)
INSERT INTO notations (event_id, membre_id, ambiance, proprete, organisation, fair_play, niveau_joueurs, date_notation)
VALUES (1, 1, 5, 4, 5, 5, 4, '2025-02-16 10:00:00'),   -- Alice (ADMIN) note Event 1
       (2, 1, 5, 5, 4, 5, 3, '2025-03-21 10:00:00'),   -- Alice (ADMIN) note Event 2
       (7, 5, 5, 4, 4, 5, 4, '2025-03-02 09:00:00'),   -- Eva (MEMBRE) note Event 7
       (7, 6, 4, 5, 5, 4, 3, '2025-03-02 10:00:00'),
       -- Nouvelles notations par des MEMBRES pour des événements PASSÉS (1, 2, 10, 13)
       (1, 4, 5, 4, 5, 5, 4, '2025-02-17 09:00:00'),   -- David (MEMBRE) note Event 1 (Badminton)
       (10, 13, 3, 5, 4, 5, 2, '2025-04-13 10:00:00'), -- Isabelle (MEMBRE) note Event 10 (Nettoyage)
       (10, 15, 4, 5, 5, 5, 1, '2025-04-13 11:00:00'), -- Karim (MEMBRE) note Event 10 (Nettoyage)
       (13, 12, 4, 3, 3, 4, 2, '2025-04-02 11:00:00'),
       -- Ajoutez ces lignes après vos `VALUES` existants pour la table `notations`

       (16, 17, 4, 5, 4, 5, 3, '2025-02-23 09:00:00'), -- Membre 17 note Event 16 (Tournoi Volley passé)
       (20, 20, 5, 4, 5, 4, 4, '2025-03-08 10:00:00'), -- Membre 20 note Event 20 (Soirée Jeux passée)
       (2, 4, 4, 3, 4, 5, 3, '2025-03-21 12:00:00'),   -- David (4) note aussi Event 2 (Soirée Mars passée)
       (1, 5, 5, 5, 4, 5, 4, '2025-02-16 11:00:00'),   -- Eva (5) note Event 1 (Badminton passé)
       (10, 12, 4, 5, 4, 5, 2, '2025-04-13 12:00:00'), -- Hélène (12) note aussi Event 10 (Nettoyage passé)
       (1, 6, 4, 4, 5, 4, 4, '2025-02-17 10:00:00'),   -- Ethelda (6) note Event 1
       (13, 5, 5, 3, 4, 5, 3, '2025-04-02 12:00:00');
-- Eva (5) note aussi Event 13 (Apéro passé)

-- Hélène (MEMBRE) note Event 13 (Apéro)
-- Franck (ADMIN) note Event 7

-- ============================================================
-- == FIN DES DONNÉES DE TEST (NETTOYÉES ET COHÉRENTES)      ==
-- ============================================================
