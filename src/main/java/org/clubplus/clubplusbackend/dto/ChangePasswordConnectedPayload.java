package org.clubplus.clubplusbackend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordConnectedPayload {
    private String currentPassword;
    private String newPassword;
}