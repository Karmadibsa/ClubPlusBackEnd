package org.clubplus.clubplusbackend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordPayload { // DTO pour le corps de la requête
    public String token;
    public String newPassword;
}