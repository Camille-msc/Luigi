package com.example.applicationrftgcma.task;

// tâche asynchrone pour supprimer un item du panier via DELETE /cart/{rentalId}
// le rentalId est intégré dans l'url - pas de body pour un DELETE rest
// la suppression sqlite n'est effectuée qu'après confirmation du serveur (via le callback)

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.manager.UrlManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@SuppressWarnings("deprecation")
public class RemoveFromCartTask extends AsyncTask<Void, Void, Boolean> {

    private static final String TAG = "RemoveFromCartTask";

    private Context context;
    // rentalId stocké en sqlite depuis l'ajout au panier - nécessaire pour cibler le bon rental côté serveur
    private int rentalId;

    public interface RemoveFromCartCallback {
        void onRemoveSuccess();
        void onRemoveError(String errorMessage);
    }

    private RemoveFromCartCallback callback;

    public RemoveFromCartTask(Context context, int rentalId, RemoveFromCartCallback callback) {
        this.context = context;
        this.rentalId = rentalId;
        this.callback = callback;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            URL url = new URL(UrlManager.getURLConnexion() + "/cart/" + rentalId);
            Log.d(TAG, "URL appelée: " + url.toString());
            return appelerReseauDelete(url);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la suppression du rental", e);
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (callback == null) return;

        if (success) {
            callback.onRemoveSuccess();
        } else {
            callback.onRemoveError("Erreur lors de la suppression du film");
        }
    }

    private Boolean appelerReseauDelete(URL url) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + getJwt());

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            return responseCode == HttpURLConnection.HTTP_OK;
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
