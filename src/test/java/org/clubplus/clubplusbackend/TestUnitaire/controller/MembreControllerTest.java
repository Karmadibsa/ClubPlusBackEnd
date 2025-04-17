package org.clubplus.clubplusbackend.TestUnitaire.controller;

import org.clubplus.clubplusbackend.TestUnitaire.mock.security.MockSecurityService;
import org.clubplus.clubplusbackend.TestUnitaire.mock.service.MockMembreService;
import org.clubplus.clubplusbackend.controller.MembreController;
import org.junit.jupiter.api.Test;

class MembreControllerTest {

    @Test
    void callFindAll_shouldSend200ok() {

        MembreController membreController = new MembreController(
                new MockMembreService(), new MockSecurityService()
        );


    }
}
