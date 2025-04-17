package org.clubplus.clubplusbackend.TestUnitaire.mock.security;

import org.clubplus.clubplusbackend.model.DemandeAmi;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Reservation;
import org.clubplus.clubplusbackend.security.ISecurityService;

import java.util.Optional;

public class MockSecurityService implements ISecurityService {
    @Override
    public Integer getCurrentUserIdOrThrow() {
        return 0;
    }

    @Override
    public Membre getCurrentMembreOrThrow() {
        return null;
    }


    @Override
    public boolean isOwner(Integer ownerId) {
        return false;
    }

    @Override
    public void checkIsOwnerOrThrow(Integer ownerId) {

    }

    @Override
    public boolean isCurrentUserMemberOfClub(Integer clubId) {
        return false;
    }

    @Override
    public void checkIsCurrentUserMemberOfClubOrThrow(Integer clubId) {

    }

    @Override
    public boolean isManagerOfClub(Integer clubId) {
        return false;
    }

    @Override
    public void checkManagerOfClubOrThrow(Integer clubId) {

    }

    @Override
    public boolean isActualAdminOfClub(Integer clubId) {
        return false;
    }

    @Override
    public void checkIsActualAdminOfClubOrThrow(Integer clubId) {

    }

    @Override
    public boolean isManagerOfEventClub(Integer eventId) {
        return false;
    }

    @Override
    public void checkManagerOfEventClubOrThrow(Integer eventId) {

    }

    @Override
    public boolean isMemberOfEventClub(Integer eventId) {
        return false;
    }

    @Override
    public void checkMemberOfEventClubOrThrow(Integer eventId) {

    }

    @Override
    public boolean isOwnerOrManagerForReservation(Reservation reservation) {
        return false;
    }

    @Override
    public void checkIsOwnerOrManagerOfAssociatedClubOrThrow(Reservation reservation) {

    }

    @Override
    public void checkIsRecepteurOfDemandeOrThrow(DemandeAmi demande) {

    }

    @Override
    public void checkIsEnvoyeurOfDemandeOrThrow(DemandeAmi demande) {

    }

    @Override
    public void checkIsOwnerOrGlobalAdminOrThrow(Integer targetUserId) {

    }

    @Override
    public Optional<Membre> getCurrentMembreOptional() {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getCurrentUserIdOptional() {
        return Optional.empty();
    }

    @Override
    public void checkIsOwnerOrAdminOfClubOrThrow(Integer notationOwnerId, Integer clubId) {

    }
}
