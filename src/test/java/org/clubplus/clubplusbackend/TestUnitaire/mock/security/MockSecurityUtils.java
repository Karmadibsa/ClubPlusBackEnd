package org.clubplus.clubplusbackend.TestUnitaire.mock.security;

import org.clubplus.clubplusbackend.security.AppUserDetails;
import org.clubplus.clubplusbackend.security.ISecurityUtils;

public class MockSecurityUtils implements ISecurityUtils {

    @Override
    public String getRole(AppUserDetails userDetails) {
        return "";
    }

    @Override
    public String generateToken(AppUserDetails userDetails) {
        return "";
    }

    @Override
    public String getSubjectFromJwt(String jwt) {
        return "";
    }
}
