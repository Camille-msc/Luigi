package com.example.applicationrftgcma.activity;

// écran de connexion - point d'entrée de l'application
// la session est effacée à chaque lancement pour forcer la reconnexion,
// car les tokens peuvent expirer entre deux sessions

import androidx.appcompat.app.AppCompatActivity;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.manager.TokenManager;
import com.example.applicationrftgcma.manager.UrlManager;
import com.example.applicationrftgcma.task.LoginTask;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private EditText etEmail;
    private EditText etPassword;
    private EditText editTextURL;
    private ProgressDialog progressDialog;
    private String[] listeURLs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        editTextURL = findViewById(R.id.editTextURL);

        listeURLs = getResources().getStringArray(R.array.listeURLs);
        Spinner spinnerURLs = findViewById(R.id.spinnerURLs);
        spinnerURLs.setOnItemSelectedListener(this);
        ArrayAdapter<CharSequence> adapterListeURLs = ArrayAdapter.createFromResource(
                this, R.array.listeURLs, android.R.layout.simple_spinner_item);
        adapterListeURLs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerURLs.setAdapter(adapterListeURLs);

        // pré-rempli pour accélérer les tests - à retirer en production
        etEmail.setText("cma@cma.com");
        etPassword.setText("password");

        // on efface la session à chaque lancement pour éviter de rester connecté
        // avec un token expiré côté serveur
        TokenManager.getInstance(this).clearToken();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        editTextURL.setText(listeURLs[position]);
        UrlManager.setURLConnexion(listeURLs[position]);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public void ouvrirPageListefilms(View view) {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Veuillez saisir votre email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Veuillez saisir votre mot de passe", Toast.LENGTH_SHORT).show();
            return;
        }

        // si l'utilisateur a modifié l'url manuellement dans le champ texte, on la prend en compte
        String urlSaisie = editTextURL.getText().toString().trim();
        if (!urlSaisie.isEmpty()) {
            UrlManager.setURLConnexion(urlSaisie);
        }

        // l'api attend le mot de passe hashé en md5, pas en clair
        // le hash est fait ici pour que LoginTask reste générique et n'ait pas à connaître ce détail
        JSONObject body;
        try {
            body = new JSONObject();
            body.put("email", email);
            body.put("password", encrypterChaineMD5(password));
        } catch (JSONException e) {
            Toast.makeText(this, "Erreur interne lors de la préparation des données", Toast.LENGTH_SHORT).show();
            return;
        }

        // non-annulable pour éviter qu'un double-clic déclenche deux requêtes en parallèle
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Connexion en cours...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        LoginTask loginTask = new LoginTask(this, body, new LoginTask.LoginCallback() {
            @Override
            public void onLoginSuccess(Integer customerId) {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                // "logged_in" est un marqueur symbolique - il n'y a pas de jwt retourné par cette api
                TokenManager tokenManager = TokenManager.getInstance(MainActivity.this);
                tokenManager.saveCustomerId(customerId);
                tokenManager.saveToken("logged_in");

                Toast.makeText(MainActivity.this, "Connexion réussie! (Customer ID: " + customerId + ")", Toast.LENGTH_SHORT).show();

                // finish() pour que le bouton retour ne revienne pas sur l'écran de connexion
                Intent intent = new Intent(MainActivity.this, ListefilmsActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onLoginError(String errorMessage) {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        loginTask.execute();
    }

    // hash md5 du mot de passe en hexadécimal
    // md5 est imposé par l'api côté serveur - on ne peut pas utiliser bcrypt ou autre
    // les bytes négatifs produisent 8 caractères hex via toHexString (ex: "ffffff80"),
    // on prend donc uniquement les 2 derniers pour obtenir la représentation correcte sur 1 byte
    private String encrypterChaineMD5(String chaine) {
        byte[] chaineBytes = chaine.getBytes();
        byte[] hash = null;
        try {
            hash = MessageDigest.getInstance("MD5").digest(chaineBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        StringBuffer hashString = new StringBuffer();
        for (int i = 0; i < hash.length; ++i) {
            String hex = Integer.toHexString(hash[i]);
            if (hex.length() == 1) {
                hashString.append('0');
                hashString.append(hex.charAt(hex.length() - 1));
            } else {
                hashString.append(hex.substring(hex.length() - 2));
            }
        }
        return hashString.toString();
    }
}
