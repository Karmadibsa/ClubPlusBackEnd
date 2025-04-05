package org.clubplus.clubplusbackend.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.clubplus.clubplusbackend.view.GlobalView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView({GlobalView.ReservationView.class, GlobalView.MembreView.class, GlobalView.EventView.class, GlobalView.CategorieView.class})
    private Long id;

    @ManyToOne
    @JoinColumn(name = "membre_id", nullable = false)
    @JsonView({GlobalView.ReservationView.class, GlobalView.EventView.class, GlobalView.CategorieView.class})
    private Membre membre;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    @JsonView({GlobalView.ReservationView.class, GlobalView.MembreView.class})
    private Event event;

    @ManyToOne
    @JoinColumn(name = "categorie_id", nullable = false)
    @JsonView({GlobalView.ReservationView.class, GlobalView.MembreView.class, GlobalView.EventView.class})
    private Categorie categorie;

    @Column(nullable = false)
    @JsonView({GlobalView.ReservationView.class, GlobalView.MembreView.class, GlobalView.EventView.class, GlobalView.CategorieView.class})
    private LocalDateTime dateReservation;


    // Ce champ ne sera pas stocké en base de données
    @Transient
    @JsonView(GlobalView.ReservationView.class)
    private String qrcodeurl;

    public Reservation(Membre membre, Event event, Categorie categorie) {
        this.membre = membre;
        this.event = event;
        this.categorie = categorie; // Cette ligne est-elle présente ?
        this.dateReservation = LocalDateTime.now();
    }


    // Getter pour qrcodeurl qui calcule dynamiquement la valeur
    public String getQrcodeurl() {
        if (qrcodeurl == null) {
            qrcodeurl = generateQrCodeUrl();
        }
        return qrcodeurl;
    }

    // Méthode privée pour générer l'URL du QR code
    private String generateQrCodeUrl() {
        try {
            if (id == null || event == null || membre == null || dateReservation == null) {
                return null;
            }

            String dateFormatted = dateReservation.format(DateTimeFormatter.ISO_DATE_TIME);
            String nomComplet = membre.getPrenom() + "_" + membre.getNom();

            // Version encodée pour URL
            String encodedNom = URLEncoder.encode(nomComplet, StandardCharsets.UTF_8.toString());
            String encodedDate = URLEncoder.encode(dateFormatted, StandardCharsets.UTF_8.toString());

            // Construire l'URL du QR code
            return String.format("res:%d|evt:%d|date:%s|user:%s",
                    id, event.getId(), encodedDate, encodedNom);
        } catch (Exception e) {
            // En cas d'erreur, retourner une chaîne par défaut
            return "error-generating-qrcode-url";
        }
    }
}
