package com.example.applicationrftgcma.manager;

// façade singleton vers DatabaseHelper - les activités passent par ici pour toutes les opérations panier
// avantage : si on change le stockage (room, firebase...) seul ce fichier change,
// les activités ne connaissent pas sqlite

import android.content.Context;

import com.example.applicationrftgcma.helper.DatabaseHelper;
import com.example.applicationrftgcma.model.Film;
import java.util.List;

public class PanierManager {

    private static PanierManager instance;
    private DatabaseHelper databaseHelper;

    // on passe le contexte applicatif (pas l'activité) pour éviter les fuites mémoire
    // si une activité est détruite pendant que le singleton est encore référencé
    private PanierManager(Context context) {
        databaseHelper = new DatabaseHelper(context.getApplicationContext());
    }

    public static synchronized PanierManager getInstance(Context context) {
        if (instance == null) {
            instance = new PanierManager(context);
        }
        return instance;
    }

    // retourne false si le film est déjà dans le panier (doublon interdit)
    public boolean ajouterFilm(Film film) {
        return databaseHelper.ajouterFilm(film);
    }

    public List<Film> obtenirFilms() {
        return databaseHelper.obtenirFilms();
    }

    public boolean supprimerFilm(int filmId) {
        return databaseHelper.supprimerFilm(filmId);
    }

    public void viderPanier() {
        databaseHelper.viderPanier();
    }

    public int getNombreFilms() {
        return databaseHelper.getNombreFilms();
    }
}
