package org.clubplus.clubplusbackend.service;

import org.clubplus.clubplusbackend.model.Adhesion;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.Role;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

public interface IMembreService {
    @Transactional(readOnly = true)
    Membre getMembreByIdOrThrow(Integer id);

    @Transactional(readOnly = true)
    Membre getMembreByIdWithSecurityCheck(Integer id);

    // --- Inscription (Logique déjà revue, semble OK) ---
    Membre registerMembreAndJoinClub(Membre membreData, String codeClub);

    // --- Mise à Jour Profil (Utilisateur Courant) ---
    Membre updateMyProfile(Membre membreDetails);

    // --- Suppression Compte (Utilisateur Courant) ---
    void deleteMyAccount();

    // --- Gestion Adhésions Club (Utilisateur Courant) ---
    Adhesion joinClub(String codeClub);

    void leaveClub(Integer clubId);

    @Transactional(readOnly = true)
    Set<Club> findClubsForCurrentUser();

    // --- Gestion Rôles (par Admin de Club) ---
    Membre changeMemberRoleInClub(Integer targetMemberId, Integer clubId, Role newRole);
}
