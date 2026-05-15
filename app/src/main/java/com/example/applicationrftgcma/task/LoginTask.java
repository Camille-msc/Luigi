package com.example.applicationrftgcma.task;

// tâche asynchrone pour authentifier un utilisateur via POST /customers/verify
// le body est formé dans MainActivity (md5 déjà calculé) - cette task ne connaît pas le format du mot de passe

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
public class LoginTask extends AsyncTask<Void, Void, Integer> {

    private static final String TAG = "LoginTask";

    private Context context;
    // body formé dans MainActivity : { "email": "...", "password": "hash_md5..." }
    private JSONObject body;
    private LoginCallback callback;

    public interface LoginCallback {
        void onLoginSuccess(Integer customerId);
        void onLoginError(String errorMessage);
    }

    public LoginTask(Context context, JSONObject body, LoginCallback callback) {
        this.context = context;
        this.body = body;
        this.callback = callback;
    }

    @Override
    protected Integer doInBackground(Void... voids) {
        try {
            URL url = new URL(UrlManager.getURLConnexion() + "/customers/verify");
            Log.d(TAG, "URL appelée: " + url.toString());

            String responseJson = appelerReseauPost(url, body);

            if (responseJson == null) {
                Log.e(TAG, "Login failed: réponse nulle de appelerReseauPost");
                return -1;
            }

            JSONObject jsonResponse = new JSONObject(responseJson);
            int customerId = jsonResponse.getInt("customerId");
            Log.d(TAG, "Response customerId: " + customerId);

            // l'api retourne customerId > 0 si la connexion réussit, -1 si les identifiants sont incorrects
            if (customerId > 0) {
                Log.d(TAG, "Login successful. CustomerId: " + customerId);
                return customerId;
            } else {
                Log.e(TAG, "Login failed: identifiants incorrects (customerId = " + customerId + ")");
                return -1;
            }

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du login", e);
            return -1;
        }
    }

    @Override
    protected void onPostExecute(Integer customerId) {
        if (callback != null) {
            if (customerId != null && customerId > 0) {
                callback.onLoginSuccess(customerId);
            } else {
                callback.onLoginError("Échec de connexion. Vérifiez vos identifiants.");
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

            if (responseCode == 401) {
                Log.e(TAG, "Non autorisé (401)");
                return null;
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return lireReponse(connection);
            } else {
                Log.e(TAG, "Erreur HTTP: " + responseCode);
                return null;
            }
        } finally {
            // toujours libérer la connexion pour éviter les fuites de ressources réseau
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
