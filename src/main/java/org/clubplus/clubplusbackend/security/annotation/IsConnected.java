package org.clubplus.clubplusbackend.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

/**
 * Annotation de sécurité personnalisée servant de raccourci pour
 * {@code @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_RESERVATION','ROLE_MEMBRE')")}.
 * <p>
 * En plaçant {@code @IsConnected} sur une méthode ou une classe de contrôleur, on restreint son accès
 * aux utilisateurs qui possèdent au moins l'un des rôles principaux de l'application (ADMIN, RESERVATION, ou MEMBRE).
 * Cela revient à vérifier que l'utilisateur est un membre authentifié et actif, par opposition
 * à un utilisateur anonyme ou un compte désactivé (qui aurait le rôle ANONYME).
 * <p>
 * Cette approche améliore la lisibilité et centralise la définition de ce que signifie "être connecté".
 *
 * <ul>
 * <li><b>@Target:</b> L'annotation peut être appliquée sur des méthodes ou des classes entières.</li>
 * <li><b>@Retention:</b> L'annotation est conservée jusqu'au moment de l'exécution (RUNTIME) pour que Spring puisse l'analyser.</li>
 * <li><b>@Inherited:</b> Si une classe est annotée, les sous-classes héritent de cette annotation de sécurité.</li>
 * </ul>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_RESERVATION','ROLE_MEMBRE')")
public @interface IsConnected {
}
