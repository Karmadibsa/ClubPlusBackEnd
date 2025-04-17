package org.clubplus.clubplusbackend.TestUnitaire.mock.service;

import org.clubplus.clubplusbackend.model.Adhesion;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.security.Role;
import org.clubplus.clubplusbackend.service.IMembreService;

import java.util.Set;

public class MockMembreService implements IMembreService {

    @Override
    public Membre getMembreByIdOrThrow(Integer id) {
        return null;
    }

    @Override
    public Membre getMembreByIdWithSecurityCheck(Integer id) {
        return null;
    }

    @Override
    public Membre registerMembreAndJoinClub(Membre membreData, String codeClub) {
        return null;
    }

    @Override
    public Membre updateMyProfile(Membre membreDetails) {
        return null;
    }

    @Override
    public void deleteMyAccount() {

    }

    @Override
    public Adhesion joinClub(String codeClub) {
        return null;
    }

    @Override
    public void leaveClub(Integer clubId) {

    }

    @Override
    public Set<Club> findClubsForCurrentUser() {
        return Set.of();
    }

    @Override
    public Membre changeMemberRoleInClub(Integer targetMemberId, Integer clubId, Role newRole) {
        return null;
    }
}
