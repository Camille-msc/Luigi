package com.example.applicationrftgcma.task;

// tâche asynchrone pour vider tout le panier via DELETE /cart/clear/{customerId}
// supprime côté serveur tous les rentals du client avec status_id = 2 (dans le panier)
// même logique que RemoveFromCartTask - le sqlite n'est vidé qu'après confirmation serveur

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
public class ClearCartTask extends AsyncTask<Void, Void, Boolean> {

    private static final String TAG = "ClearCartTask";

    private Context context;
    private int customerId;

    public interface ClearCartCallback {
        void onClearSuccess();
        void onClearError(String errorMessage);
    }

    private ClearCartCallback callback;

    public ClearCartTask(Context context, int customerId, ClearCartCallback callback) {
        this.context = context;
        this.customerId = customerId;
        this.callback = callback;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            URL url = new URL(UrlManager.getURLConnexion() + "/cart/clear/" + customerId);
            Log.d(TAG, "URL appelée: " + url.toString());
            return appelerReseauDelete(url);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du vidage du panier", e);
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (callback == null) return;

        if (success) {
            callback.onClearSuccess();
        } else {
            callback.onClearError("Erreur lors du vidage du panier");
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
