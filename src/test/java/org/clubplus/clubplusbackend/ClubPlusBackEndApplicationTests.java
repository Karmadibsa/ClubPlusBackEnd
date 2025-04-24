package org.clubplus.clubplusbackend;

import org.clubplus.clubplusbackend.dao.MembreDao;
import org.clubplus.clubplusbackend.security.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    @MockitoBean
    private SecurityService securityService;
    @MockitoBean
    private MembreDao membreRepository;

    @BeforeEach
    public void setup() {
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }


    @Test
    @WithMockUser(username = "bob.membre@email.com", roles = {"MEMBRE"})
    public void getAdminOfCLub() throws Exception {

        mvc.perform(get("/api/clubs/1/admin"))
                .andExpect(status().isOk());

    }

//    @Test
//    @WithMockUser(username = "bob.membre@email.com", roles = {"MEMBRE"})
//    public void getAdminOfClub_ShouldReturnOk_WhenMocksAreSetUp() throws Exception {
//        // --- Arrange ---
//        Membre mockAdmin = new Membre();
//        mockAdmin.setId(1);
//        mockAdmin.setEmail("admin.club1@email.com");

    /// /        /* // Configuration Option B (identique aussi)
//        doNothing().when(securityService).checkIsCurrentUserMemberOfClubOrThrow(1);
//        when(membreRepository.findAdminByClubId(1)).thenReturn(Optional.of(mockAdmin));
//
//        // --- Act & Assert (identique) ---
//        mvc.perform(get("/api/clubs/1/admin"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(1))
//                .andExpect(jsonPath("$.email").value("admin.club1@email.com"));
//    }
    @Test
    @WithMockUser(username = "bob.membre@email.com", roles = {"MEMBRE"})
    public void getAllEventOfMyCLubs() throws Exception {

        mvc.perform(get("/api/events"))
                .andExpect(status().isOk());

    }
//    current user is nul donc erreur 500
}
