package org.clubplus.clubplusbackend.security;

import org.clubplus.clubplusbackend.model.DemandeAmi;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Reservation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ISecurityService {
    Integer getCurrentUserIdOrThrow();

    @Transactional(readOnly = true)
    Membre getCurrentMembreOrThrow();

    boolean isOwner(Integer ownerId);

    void checkIsOwnerOrThrow(Integer ownerId);

    @Transactional(readOnly = true)
    boolean isCurrentUserMemberOfClub(Integer clubId);

    void checkIsCurrentUserMemberOfClubOrThrow(Integer clubId);

    @Transactional(readOnly = true)
    boolean isManagerOfClub(Integer clubId);

    void checkManagerOfClubOrThrow(Integer clubId);

    @Transactional(readOnly = true)
    boolean isActualAdminOfClub(Integer clubId);

    void checkIsActualAdminOfClubOrThrow(Integer clubId);

    @Transactional(readOnly = true)
    boolean isManagerOfEventClub(Integer eventId);

    void checkManagerOfEventClubOrThrow(Integer eventId);

    @Transactional(readOnly = true)
    boolean isMemberOfEventClub(Integer eventId);

    void checkMemberOfEventClubOrThrow(Integer eventId);

    @Transactional(readOnly = true)
    boolean isOwnerOrManagerForReservation(Reservation reservation);

    void checkIsOwnerOrManagerOfAssociatedClubOrThrow(Reservation reservation);

    void checkIsRecepteurOfDemandeOrThrow(DemandeAmi demande);

    void checkIsEnvoyeurOfDemandeOrThrow(DemandeAmi demande);

    void checkIsOwnerOrGlobalAdminOrThrow(Integer targetUserId);

    Optional<Membre> getCurrentMembreOptional();

    Optional<Integer> getCurrentUserIdOptional();

    void checkIsOwnerOrAdminOfClubOrThrow(Integer notationOwnerId, Integer clubId);
}
