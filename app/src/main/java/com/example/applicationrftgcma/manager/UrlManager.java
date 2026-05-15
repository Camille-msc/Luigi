package com.example.applicationrftgcma.manager;

// stockage global de l'url du serveur rest - uniquement des méthodes statiques,
// pas de singleton nécessaire car il n'y a qu'une seule valeur à partager

public class UrlManager {

    // 10.0.2.2 est l'adresse spéciale de l'émulateur android pour atteindre localhost de la machine hôte
    // (l'émulateur ne peut pas utiliser "localhost" directement)
    private static String URLConnexion = "http://10.0.2.2:8180";

    public static String getURLConnexion() {
        return URLConnexion;
    }

    // l'url peut être changée en cours d'exécution depuis le spinner ou le champ texte de MainActivity
    // toutes les tasks réseau lancées ensuite utiliseront automatiquement la nouvelle valeur
    public static void setURLConnexion(String url) {
        URLConnexion = url;
    }
}
