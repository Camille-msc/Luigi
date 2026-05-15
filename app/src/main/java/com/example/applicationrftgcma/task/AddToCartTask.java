package com.example.applicationrftgcma.task;

// tâche asynchrone pour ajouter un film au panier via POST /cart/add
// crée un rental côté serveur avec status_id = 2 (dans le panier)
// retourne "UNAVAILABLE" sur 404 pour distinguer "film non dispo" d'une vraie erreur réseau

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.manager.UrlManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@SuppressWarnings("deprecation")
public class AddToCartTask extends AsyncTask<Void, Void, String> {

    private static final String TAG = "AddToCartTask";

    private Context context;
    // body formé dans l'appelant : { "customerId": N, "filmId": M }
    private JSONObject body;

    public interface AddToCartCallback {
        // rentalId retourné par l'api - à stocker en sqlite pour pouvoir supprimer le rental plus tard
        void onAddToCartSuccess(int rentalId);
        void onAddToCartError(String errorMessage);
    }

    private AddToCartCallback callback;

    public AddToCartTask(Context context, JSONObject body, AddToCartCallback callback) {
        this.context = context;
        this.body = body;
        this.callback = callback;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            URL url = new URL(UrlManager.getURLConnexion() + "/cart/add");
            Log.d(TAG, "URL appelée: " + url.toString());
            return appelerReseauPost(url, body);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'ajout au panier", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (callback == null) return;

        if ("UNAVAILABLE".equals(result)) {
            callback.onAddToCartError("Aucun exemplaire disponible pour ce film");
        } else if (result == null) {
            callback.onAddToCartError("Erreur de connexion au serveur");
        } else {
            // succès - la réponse contient le rental créé : { "rental": { "rentalId": 42, ... } }
            try {
                JSONObject json = new JSONObject(result);
                JSONObject rental = json.getJSONObject("rental");
                int rentalId = rental.getInt("rentalId");
                callback.onAddToCartSuccess(rentalId);
            } catch (Exception e) {
                Log.e(TAG, "Erreur parsing rentalId", e);
                callback.onAddToCartError("Erreur lors du traitement de la réponse");
            }
        }
    }

    private String appelerReseauPost(URL url, JSONObject body) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + getJwt());
            connection.setDoOutput(true);

            OutputStream os = connection.getOutputStream();
            os.write(body.toString().getBytes("UTF-8"));
            os.close();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                return lireReponse(connection);
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                // 404 = aucun exemplaire disponible pour ce film - code sémantique distinct de null
                Log.e(TAG, "Film indisponible (404)");
                return "UNAVAILABLE";
            } else if (responseCode == 401) {
                Log.e(TAG, "Non autorisé (401)");
                return null;
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

    private String getJwt() {
        return context.getResources().getString(R.string.api_jwt_token);
    }
}
