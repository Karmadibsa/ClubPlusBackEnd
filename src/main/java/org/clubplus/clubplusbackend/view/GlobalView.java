package org.clubplus.clubplusbackend.view;

public class GlobalView {
    // Vue pour les informations de base
    public interface Base {
    }

    // Vue pour les événements (sans catégories détaillées)
    public interface EventView extends Base {
    }

    // Vue pour les catégories (sans événements détaillés)
    public interface CategorieView extends Base {
    }

    // Nouvelle vue pour les réservations
    public interface ReservationView extends Base {
    }

    // Vue pour les membres
    public interface MembreView extends Base {
    }
}

