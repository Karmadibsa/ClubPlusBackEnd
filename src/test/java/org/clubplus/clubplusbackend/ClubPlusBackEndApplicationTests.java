package org.clubplus.clubplusbackend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ClubPlusBackEndApplicationTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    private WebApplicationContext context;
    private MockMvc mvc;


    @BeforeEach
    public void setup() {
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }


    @Test
    @WithUserDetails("bob.membre@email.com")
    public void getAdminOfCLub() throws Exception {

        mvc.perform(get("/api/clubs/1/admin"))
                .andExpect(status().isOk());

    }
    
    @Test
    @WithUserDetails("bob.membre@email.com")
    public void getAllEventOfMyCLubs() throws Exception {

        mvc.perform(get("/api/events"))
                .andExpect(status().isOk());

    }
//    current user is nul donc erreur 500
}
