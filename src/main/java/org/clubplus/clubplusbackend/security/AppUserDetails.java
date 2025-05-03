package org.clubplus.clubplusbackend.security;

import lombok.Getter;
import org.clubplus.clubplusbackend.model.Adhesion;
import org.clubplus.clubplusbackend.model.Club;
import org.clubplus.clubplusbackend.model.Membre;
import org.clubplus.clubplusbackend.model.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

/**
 * Implémentation personnalisée de l'interface {@link UserDetails} de Spring Security.
 * Cette classe encapsule l'entité {@link Membre} de l'application pour l'adapter
 * au contexte de sécurité de Spring. Elle fournit les informations nécessaires
 * à Spring Security pour l'authentification et l'autorisation, telles que
 * le nom d'utilisateur (email), le mot de passe encodé, les autorités (rôles),
 * et le statut du compte.
 * Inclut également des méthodes utilitaires pour accéder facilement à l'ID du membre
 * et à l'ID du club potentiellement géré par l'utilisateur.
 *
 * @see UserDetails
 * @see Membre
 * @see GrantedAuthority
 */
@Getter // Lombok génère les getters pour les champs (ici, principalement pour 'membre')
public class AppUserDetails implements UserDetails {

    /**
     * L'entité {@link Membre} sous-jacente représentant l'utilisateur dans le domaine de l'application.
     * Cette instance contient toutes les informations de l'utilisateur (ID, email, mot de passe, rôle, etc.).
     * Elle est définie comme 'final' pour garantir son immutabilité après la construction de l'objet AppUserDetails.
     */
    private final Membre membre;

    /**
     * Construit une nouvelle instance de {@code AppUserDetails} basée sur une entité {@link Membre}.
     * Vérifie que le membre fourni n'est pas null.
     *
     * @param membre L'entité {@link Membre} à encapsuler. Ne doit pas être null.
     * @throws NullPointerException si l'objet {@code membre} fourni est null.
     */
    public AppUserDetails(Membre membre) {
        // Vérification essentielle pour garantir l'intégrité de l'objet UserDetails.
        Objects.requireNonNull(membre, "L'entité Membre ne peut pas être null pour créer AppUserDetails.");
        this.membre = membre;
    }

    /**
     * Retourne les autorités (rôles) accordées à l'utilisateur.
     * Basé sur le champ {@code role} de l'entité {@link Membre}.
     * L'autorité est formatée avec le préfixe "ROLE_" conformément aux conventions de Spring Security.
     *
     * @return Une collection contenant une seule {@link GrantedAuthority} représentant le rôle global
     * de l'utilisateur (ex: "ROLE_ADMIN", "ROLE_MEMBRE"), ou une collection vide si
     * l'entité Membre ou son rôle est null.
     * @see SimpleGrantedAuthority
     * @see Role
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Gestion du cas où le membre ou son rôle serait invalide.
        if (this.membre == null || this.membre.getRole() == null) {
            return Collections.emptyList(); // Pas d'autorités si pas de rôle défini.
        }
        // Construction de l'autorité avec le préfixe standard "ROLE_".
        String roleName = "ROLE_" + this.membre.getRole().name(); // Ex: "ROLE_ADMIN"
        // Retourne une liste immuable contenant cette unique autorité.
        return Collections.singletonList(new SimpleGrantedAuthority(roleName));
    }

    /**
     * Retourne le mot de passe utilisé pour authentifier l'utilisateur.
     * Le mot de passe retourné doit être celui stocké en base de données (généralement encodé).
     *
     * @return Le mot de passe encodé de l'utilisateur tel que stocké dans l'entité {@link Membre}.
     * Retourne {@code null} si l'instance {@code membre} est null (cas théorique vu le constructeur).
     */
    @Override
    public String getPassword() {
        // Accès direct au mot de passe stocké dans l'entité Membre.
        return (this.membre != null) ? membre.getPassword() : null;
    }

    /**
     * Retourne le nom d'utilisateur utilisé pour authentifier l'utilisateur.
     * Dans cette implémentation, l'adresse email est utilisée comme nom d'utilisateur unique.
     *
     * @return L'adresse email de l'utilisateur telle que stockée dans l'entité {@link Membre}.
     * Retourne {@code null} si l'instance {@code membre} est null (cas théorique).
     */
    @Override
    public String getUsername() {
        // Utilisation de l'email comme identifiant principal pour Spring Security.
        return (this.membre != null) ? membre.getEmail() : null;
    }

    /**
     * Méthode utilitaire spécifique à l'application pour récupérer facilement l'identifiant unique (ID)
     * de l'entité {@link Membre} associée à cet {@code UserDetails}.
     * Utile pour les opérations dans les couches service nécessitant l'ID de l'utilisateur connecté.
     *
     * @return L'ID (Integer) unique de l'utilisateur {@link Membre}.
     * @throws IllegalStateException si l'instance {@code membre} interne est null (ce qui ne devrait
     *                               pas arriver si l'objet a été correctement construit).
     */
    public Integer getId() {
        // Bien que le constructeur vérifie la nullité, une vérification ici reste une bonne pratique défensive.
        if (this.membre == null) {
            throw new IllegalStateException("Impossible de récupérer l'ID : l'entité Membre associée est null.");
        }
        return membre.getId();
    }

    /**
     * Méthode utilitaire spécifique à l'application pour récupérer l'identifiant (ID) de l'unique club
     * que cet utilisateur gère, basé sur son rôle (ADMIN ou RESERVATION) et ses adhésions.
     * <p>
     * <b>Hypothèse:</b> Un utilisateur ayant un rôle de gestion (ADMIN/RESERVATION) est associé
     * à un et un seul club via l'entité {@link Adhesion}. Cette méthode prend la première adhésion trouvée.
     * </p>
     * Si l'utilisateur a le bon rôle mais n'a pas d'adhésion, ou si l'adhésion n'a pas de club lié,
     * un message d'anomalie est loggué sur {@code System.err} et la méthode retourne {@code null}.
     *
     * @return L'ID (Integer) du club géré par l'utilisateur si son rôle est ADMIN ou RESERVATION
     * et qu'une adhésion valide est trouvée ; {@code null} dans tous les autres cas (rôle différent,
     * pas d'adhésion, anomalie de données).
     * @see Adhesion
     * @see Club
     * @see Role#ADMIN
     * @see Role#RESERVATION
     */
    public Integer getManagedClubId() {
        if (this.membre == null || this.membre.getRole() == null) {
            return null; // Cas invalide
        }

        Role userRole = this.membre.getRole();

        // Vérifie si l'utilisateur a un rôle de gestionnaire.
        if (userRole == Role.ADMIN || userRole == Role.RESERVATION) {
            Set<Adhesion> adhesions = this.membre.getAdhesions(); // Récupère les adhésions du membre.

            if (adhesions != null && !adhesions.isEmpty()) {
                // Utilisation de l'API Stream pour une extraction concise et sûre.
                Optional<Integer> clubIdOptional = adhesions.stream()
                        .filter(Objects::nonNull) // Ignore les adhésions nulles (sécurité)
                        .map(Adhesion::getClub)   // Extrait l'objet Club de l'adhésion
                        .filter(Objects::nonNull) // Ignore si le club lié est nul (sécurité)
                        .map(Club::getId)         // Extrait l'ID de l'objet Club
                        .findFirst();             // Prend l'ID du premier club trouvé

                if (clubIdOptional.isPresent()) {
                    return clubIdOptional.get(); // Retourne l'ID trouvé.
                }
            }
            // Loggue une anomalie si un manager n'a pas d'adhésion valide.
            System.err.println("ANOMALIE: Utilisateur " + getUsername() + " (" + userRole + ") avec rôle de gestion mais sans adhésion valide pour déterminer le club géré.");
            return null; // Aucune adhésion valide trouvée.
        }

        return null; // L'utilisateur n'a pas un rôle de gestionnaire.
    }

    // --- Implémentation des indicateurs de statut du compte UserDetails ---

    /**
     * Indique si le compte de l'utilisateur a expiré. Un compte expiré ne peut pas être authentifié.
     *
     * @return {@code true} si le compte est valide (non expiré), {@code false} s'il n'est plus valide.
     * Actuellement, retourne toujours {@code true} (pas de gestion d'expiration implémentée).
     */
    @Override
    public boolean isAccountNonExpired() {
        // À personnaliser si la logique d'expiration de compte est nécessaire.
        return true;
    }

    /**
     * Indique si l'utilisateur est verrouillé ou déverrouillé. Un compte verrouillé ne peut pas être authentifié.
     *
     * @return {@code true} si l'utilisateur n'est pas verrouillé, {@code false} sinon.
     * Actuellement, retourne toujours {@code true} (pas de gestion de verrouillage implémentée).
     */
    @Override
    public boolean isAccountNonLocked() {
        // À personnaliser si la logique de verrouillage de compte est nécessaire (ex: après trop d'échecs de connexion).
        return true;
    }

    /**
     * Indique si les informations d'identification (mot de passe) de l'utilisateur ont expiré.
     * Un mot de passe expiré empêche l'authentification.
     *
     * @return {@code true} si les informations d'identification sont valides (non expirées), {@code false} sinon.
     * Actuellement, retourne toujours {@code true} (pas de gestion d'expiration de mot de passe implémentée).
     */
    @Override
    public boolean isCredentialsNonExpired() {
        // À personnaliser si la politique d'expiration de mot de passe est nécessaire.
        return true;
    }

    /**
     * Indique si l'utilisateur est activé ou désactivé. Un utilisateur désactivé ne peut pas être authentifié.
     *
     * @return {@code true} si l'utilisateur est activé, {@code false} sinon.
     * Actuellement, retourne toujours {@code true}. Pourrait être lié à un champ `actif`
     * dans l'entité {@link Membre}.
     */
    @Override
    public boolean isEnabled() {
        // Exemple de personnalisation (à décommenter et adapter si 'isActif()' existe sur Membre):
        // if (this.membre != null) {
        //     return this.membre.isActif(); // Lie l'activation à l'état du Membre.
        // }
        // return false; // Ou retourner false si le membre est null.

        // Comportement par défaut actuel : toujours activé.
        return true;
    }
}
