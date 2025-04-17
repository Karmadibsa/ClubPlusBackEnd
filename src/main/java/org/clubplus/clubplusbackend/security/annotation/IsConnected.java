package org.clubplus.clubplusbackend.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
// #id est l'ID du club attendu comme paramètre de la méthode
@PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_RESERVATION')")
public @interface IsReservation {
}
