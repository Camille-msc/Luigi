package com.example.applicationrftgcma.task;

// tâche asynchrone pour récupérer la liste des films via GET /films
// le json brut est transmis à ListefilmsActivity qui se charge du parsing et de l'affichage -
// ce choix garde toute la logique d'affichage centralisée dans l'activité

import android.os.AsyncTask;
import android.util.Log;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.activity.ListefilmsActivity;
import com.example.applicationrftgcma.manager.TokenManager;
import com.example.applicationrftgcma.manager.UrlManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@SuppressWarnings("deprecation")
public class ListefilmsTask extends AsyncTask<Void, Void, String> {

    private static final String TAG = "ListefilmsTask";

    // volatile car la référence est lue depuis doInBackground (thread réseau) et écrite depuis le thread ui
    private volatile ListefilmsActivity screen;

    public ListefilmsTask(ListefilmsActivity s) {
        this.screen = s;
    }

    @Override
    protected void onPreExecute() {
        screen.showProgressBar(true);
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            URL url = new URL(UrlManager.getURLConnexion() + "/films");
            Log.d(TAG, "URL appelée: " + url.toString());
            return appelerReseauGet(url);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération des films", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(String resultat) {
        Log.d(TAG, "onPostExecute / taille résultat: " + (resultat != null ? resultat.length() : "null"));
        screen.mettreAJourActivityApresAppelRest(resultat);
        screen.showProgressBar(false);
    }

    private String appelerReseauGet(URL url) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + getJwt());

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            if (responseCode == 401) {
                // token expiré - on vide la session et on ferme l'activité pour forcer la reconnexion
                Log.e(TAG, "Non autorisé (401)");
                screen.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TokenManager.getInstance(screen).clearToken();
                        screen.finish();
                    }
                });
                return null;
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return lireReponse(connection);
            } else {
                Log.e(TAG, "Erreur HTTP: " + responseCode);
                return null;
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String lireReponse(HttpURLConnection connection) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();
        return response.toString();
    }

    // screen est utilisé comme contexte car ListefilmsTask ne reçoit pas de Context séparé
    private String getJwt() {
        return screen.getResources().getString(R.string.api_jwt_token);
    }
}
