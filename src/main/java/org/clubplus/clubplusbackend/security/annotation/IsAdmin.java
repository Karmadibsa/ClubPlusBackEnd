package org.clubplus.clubplusbackend.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

/**
 * Annotation de sécurité personnalisée servant de raccourci pour {@code @PreAuthorize("hasRole('ROLE_ADMIN')")}.
 * <p>
 * En plaçant {@code @IsAdmin} sur une méthode ou une classe de contrôleur, on restreint son accès
 * aux utilisateurs qui possèdent le rôle {@code ADMIN}. Spring Security interprète cette annotation
 * grâce à la méta-annotation {@code @PreAuthorize}.
 * <p>
 * Cette approche améliore la lisibilité du code et centralise la logique de permission.
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
@PreAuthorize("hasRole('ROLE_ADMIN')")
public @interface IsAdmin {
}
