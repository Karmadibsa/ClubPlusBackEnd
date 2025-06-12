package org.clubplus.clubplusbackend.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

/**
 * Annotation de sécurité personnalisée servant de raccourci pour {@code @PreAuthorize("hasRole('ROLE_MEMBRE')")}.
 * <p>
 * En plaçant {@code @IsMembre} sur une méthode ou une classe de contrôleur, on restreint son accès
 * <strong>strictement</strong> aux utilisateurs qui possèdent le rôle spécifique {@code MEMBRE}.
 * <p>
 * <strong>Attention :</strong> Contrairement à une vérification avec {@code hasAnyRole}, cette annotation
 * est très restrictive. Un utilisateur ayant un rôle supérieur (comme {@code ADMIN} ou {@code RESERVATION})
 * ne passera pas cette vérification s'il ne possède pas aussi explicitement le rôle {@code MEMBRE}.
 * Pour une vérification plus inclusive, voir {@link IsConnected}.
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
@PreAuthorize("hasRole('ROLE_MEMBRE')")
public @interface IsMembre {
}
