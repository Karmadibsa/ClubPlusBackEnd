package org.clubplus.clubplusbackend.security;

public interface ISecurityUtils {
    String getRole(AppUserDetails userDetails);

    String generateToken(AppUserDetails userDetails);

    String getSubjectFromJwt(String jwt);
}
