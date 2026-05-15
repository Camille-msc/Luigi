package com.example.applicationrftgcma.task;

// tâche asynchrone qui désérialise le json d'un film en objet Film
// le json vient de l'intent, pas du réseau - mais on utilise quand même une asynctask
// pour garder la cohérence avec l'architecture et afficher une progressbar pendant le traitement

import android.os.AsyncTask;
import android.util.Log;

import com.example.applicationrftgcma.activity.DetailfilmActivity;
import com.example.applicationrftgcma.model.Film;

import com.google.gson.Gson;

@SuppressWarnings("deprecation")
public class DetailfilmTask extends AsyncTask<String, Void, Film> {

    private static final String TAG = "DetailfilmTask";

    // volatile car doInBackground tourne sur un thread secondaire
    private volatile DetailfilmActivity screen;

    public DetailfilmTask(DetailfilmActivity s) {
        this.screen = s;
    }

    @Override
    protected void onPreExecute() {
        screen.showProgressBar(true);
    }

    @Override
    protected Film doInBackground(String... params) {
        String filmJson = params[0];
        Film film = null;

        try {
            Gson gson = new Gson();
            film = gson.fromJson(filmJson, Film.class);
            Log.d(TAG, "Film parsé avec succès : " + film.getTitre());
        } catch (Exception e) {
            Log.e(TAG, "Erreur de parsing du JSON du film : " + e.getMessage());
            // film reste null - DetailfilmActivity gère ce cas dans mettreAJourActivityAvecFilm()
        }

        return film;
    }

    @Override
    protected void onPostExecute(Film film) {
        screen.mettreAJourActivityAvecFilm(film);
        screen.showProgressBar(false);
    }
}
