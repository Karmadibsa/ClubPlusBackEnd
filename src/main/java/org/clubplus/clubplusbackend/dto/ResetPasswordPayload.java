package org.clubplus.clubplusbackend.dto;

public class ResetPasswordPayload { // DTO pour le corps de la requête
    public String token;
    public String newPassword;
}