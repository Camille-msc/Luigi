package com.example.applicationrftgcma.task;

// tâche asynchrone pour valider le panier via POST /cart/checkout
// côté serveur, change le status_id de tous les rentals du client de 2 (panier) à 3 (location active)
// la réponse contient itemsCount - le nombre de films validés, affiché dans le message de confirmation

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.activity.PanierActivity;
import com.example.applicationrftgcma.manager.UrlManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@SuppressWarnings("deprecation")
public class CheckoutTask extends AsyncTask<Void, Void, String> {

    private static final String TAG = "CheckoutTask";

    private Context context;
    // body formé dans PanierActivity : { "customerId": N }
    private JSONObject body;

    public interface CheckoutCallback {
        void onCheckoutSuccess(int itemsCount);
        void onCheckoutError(String errorMessage);
    }

    private CheckoutCallback callback;

    // PanierActivity est passé comme Context - elle hérite de Context via AppCompatActivity
    public CheckoutTask(PanierActivity activity, JSONObject body, CheckoutCallback callback) {
        this.context = activity;
        this.body = body;
        this.callback = callback;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            URL url = new URL(UrlManager.getURLConnexion() + "/cart/checkout");
            Log.d(TAG, "URL appelée: " + url.toString());
            return appelerReseauPost(url, body);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du checkout", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (callback == null) return;

        if (result != null) {
            try {
                JSONObject jsonResponse = new JSONObject(result);
                int itemsCount = jsonResponse.getInt("itemsCount");
                callback.onCheckoutSuccess(itemsCount);
            } catch (Exception e) {
                Log.e(TAG, "Erreur parsing JSON", e);
                callback.onCheckoutError("Erreur lors du traitement de la réponse");
            }
        } else {
            callback.onCheckoutError("Erreur de connexion au serveur");
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
