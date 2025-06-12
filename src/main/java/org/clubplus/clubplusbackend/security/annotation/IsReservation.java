package org.clubplus.clubplusbackend.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

/**
 * Annotation de sécurité personnalisée servant de raccourci pour
 * {@code @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_RESERVATION')")}.
 * <p>
 * En plaçant {@code @IsReservation} sur une méthode ou une classe de contrôleur, on restreint son accès
 * aux utilisateurs qui possèdent au moins l'un des rôles de gestion : {@code ADMIN} ou {@code RESERVATION}.
 * Cette annotation est typiquement utilisée pour les fonctionnalités liées à la gestion d'un club,
 * comme la création d'événements ou la consultation de statistiques.
 * <p>
 * Cette approche hiérarchique (un ADMIN étant implicitement un gestionnaire) est plus inclusive que {@link IsAdmin}.
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
@PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_RESERVATION')")
public @interface IsReservation {
}
