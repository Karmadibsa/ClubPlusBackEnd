package org.clubplus.clubplusbackend.security;

import lombok.Getter;
import org.clubplus.clubplusbackend.model.Adhesion;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Membre;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Getter
// Cette classe implémente l'interface UserDetails de Spring Security [4]
public class AppUserDetails implements UserDetails {

    // Référence à l'entité Membre [7][8]
    // protected Membre membre; // 'protected' est inhabituel, 'private final' est plus courant
    private final Membre membre; // Utiliser private final pour l'immutabilité après construction

    // Constructeur qui initialise avec Membre [3][5]
    public AppUserDetails(Membre membre) {
        // BONNE PRATIQUE: Ajouter un null check
        Objects.requireNonNull(membre, "Membre ne peut pas être null pour AppUserDetails");
        this.membre = membre;
    }

    // Retourne les autorisations/rôles [2][4]
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Vérification si rôle est null
        if (this.membre == null || this.membre.getRole() == null) {
            return Collections.emptyList(); // Pas d'autorités si pas de rôle
        }
        // Utilise le rôle global du Membre, préfixé par "ROLE_" (Convention Spring Security)
        String roleName = "ROLE_" + this.membre.getRole().name(); // .name() donne "ADMIN", "MEMBRE", etc.
        return Collections.singletonList(new SimpleGrantedAuthority(roleName)); // CORRECT
    }

    // Retourne le mot de passe encodé [4][5]
    @Override
    public String getPassword() {
        // Vérification nullité membre
        if (this.membre == null) {
            return null;
        }
        return membre.getPassword(); // CORRECT
    }

    // Retourne l'identifiant (email ici) [4][5]
    @Override
    public String getUsername() {
        // Vérification nullité membre
        if (this.membre == null) {
            return null;
        }
        return membre.getEmail(); // CORRECT
    }

    // Méthode ajoutée pour obtenir l'ID interne du Membre (pour nos services)
    public Integer getId() {
        // Vérification nullité membre
        if (this.membre == null) {
            // Lever une exception est probablement mieux ici car getId ne devrait pas être appelé
            // sur un UserDetails invalide créé à partir d'un membre null.
            throw new IllegalStateException("Membre associé à AppUserDetails est null lors de l'appel à getId().");
        }
        return membre.getId(); // CORRECT
    }

    /**
     * Récupère l'ID de l'unique club géré par cet utilisateur, si son rôle
     * est ADMIN ou RESERVATION, en utilisant le Set d'adhésions.
     *
     * @return L'ID du club géré, ou null si non applicable ou non trouvé.
     */
    public Integer getManagedClubId() {
        if (this.membre == null || this.membre.getRole() == null) {
            return null;
        }

        Role userRole = this.membre.getRole();

        if (userRole == Role.ADMIN || userRole == Role.RESERVATION) {
            // Assurez-vous que membre.getAdhesions() retourne Set<Adhesion>
            Set<Adhesion> adhesions = this.membre.getAdhesions(); // <--- Vérifier type de retour

            if (adhesions != null && !adhesions.isEmpty()) {
                // Utiliser Stream API pour trouver la première adhésion et extraire l'ID du club
                Optional<Integer> clubIdOptional = adhesions.stream()
                        .findFirst() // Prend le premier élément du Set (ordre non garanti mais OK si un seul)
                        .map(Adhesion::getClub) // Extrait le Club de l'Adhesion
                        .map(Club::getId);      // Extrait l'ID du Club

                // Si l'ID a été trouvé, le retourner, sinon retourner null
                if (clubIdOptional.isPresent()) {
                    return clubIdOptional.get();
                }
            }
            // Si aucune adhésion valide n'est trouvée (log d'anomalie)
            System.err.println("ANOMALIE: L'utilisateur " + getUsername() + " (" + userRole + ") n'a pas d'adhésion valide pour déterminer le club géré.");
            return null;
        }
        return null; // Pas ADMIN ou RESA
    }

    // !!! MÉTHODES MANQUANTES DE L'INTERFACE UserDetails !!! [4]
    // Ces méthodes DOIVENT être implémentées.
    // Par défaut, si vous n'avez pas de logique spécifique, retournez 'true'.

    @Override
    public boolean isAccountNonExpired() {
        return true; // Mettre 'false' si vous gérez l'expiration des comptes
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Mettre 'false' si vous gérez le verrouillage des comptes (ex: trop d'échecs de login)
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Mettre 'false' si vous gérez l'expiration des mots de passe
    }

    @Override
    public boolean isEnabled() {
        // Vous pourriez lier ceci à un champ 'actif' ou 'enabled' dans votre entité Membre si nécessaire.
        // if (this.membre != null) { return this.membre.isActif(); }
        return true; // Par défaut, compte activé
    }
}
