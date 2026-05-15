package com.example.applicationrftgcma.activity;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.adapter.PanierAdapter;
import com.example.applicationrftgcma.manager.PanierManager;
import com.example.applicationrftgcma.manager.TokenManager;
import com.example.applicationrftgcma.model.Film;
import com.example.applicationrftgcma.task.CheckoutTask;
import com.example.applicationrftgcma.task.ClearCartTask;
import com.example.applicationrftgcma.task.RemoveFromCartTask;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

// écran du panier - affiche les films mis en attente de location
// les données viennent du sqlite local, qui est mis à jour après chaque opération serveur
public class PanierActivity extends AppCompatActivity {

    private ListView listePanier;
    private TextView tvNombreFilms;
    private PanierAdapter adapter;
    private List<Film> films;
    private PanierManager panierManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panier);

        panierManager = PanierManager.getInstance(this);

        listePanier = findViewById(R.id.listePanier);
        tvNombreFilms = findViewById(R.id.tvNombreFilms);
        Button btnViderPanier = findViewById(R.id.btnViderPanier);
        Button btnRetourPanier = findViewById(R.id.btnRetourPanier);

        chargerPanier();

        btnRetourPanier.setOnClickListener(v -> finish());

        btnViderPanier.setOnClickListener(v -> {
            if (films.isEmpty()) {
                Toast.makeText(this, "Le panier est déjà vide", Toast.LENGTH_SHORT).show();
            } else {
                afficherDialogueConfirmationVider();
            }
        });
    }

    // onResume recharge le panier à chaque retour sur cet écran,
    // pour prendre en compte les ajouts faits depuis DetailfilmActivity ou FilmAdapter
    @Override
    protected void onResume() {
        super.onResume();
        chargerPanier();
    }

    private void chargerPanier() {
        films = panierManager.obtenirFilms();
        tvNombreFilms.setText(films.size() + " film(s)");

        // la confirmation et la mise à jour de la bdd sont gérées par l'activité, pas par l'adapter -
        // l'adapter signale juste le clic via ce callback
        adapter = new PanierAdapter(this, films, (film, position) -> {
            afficherDialogueConfirmationSupprimer(film, position);
        });

        listePanier.setAdapter(adapter);
    }

    private void afficherDialogueConfirmationSupprimer(Film film, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer du panier")
                .setMessage("Voulez-vous supprimer \"" + film.getTitre() + "\" du panier ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    // on supprime d'abord côté serveur - si ça échoue, on ne touche pas au sqlite local
                    // pour éviter d'avoir un film dans le sqlite sans rental correspondant côté serveur
                    new RemoveFromCartTask(this, film.getRentalId(), new RemoveFromCartTask.RemoveFromCartCallback() {
                        @Override
                        public void onRemoveSuccess() {
                            panierManager.supprimerFilm(film.getId());
                            films.remove(position);
                            adapter.notifyDataSetChanged();
                            tvNombreFilms.setText(films.size() + " film(s)");
                            Toast.makeText(PanierActivity.this, "Film supprimé du panier", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onRemoveError(String errorMessage) {
                            Toast.makeText(PanierActivity.this, "Erreur: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }).execute();
                })
                .setNegativeButton("Non", null)
                .show();
    }

    private void afficherDialogueConfirmationVider() {
        new AlertDialog.Builder(this)
                .setTitle("Vider le panier")
                .setMessage("Voulez-vous supprimer tous les films du panier ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    Integer customerId = TokenManager.getInstance(this).getCustomerId();
                    if (customerId == null) {
                        Toast.makeText(this, "Erreur: Customer ID non trouvé", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // même logique que pour la suppression individuelle -
                    // on vide le sqlite seulement si le serveur confirme
                    new ClearCartTask(this, customerId, new ClearCartTask.ClearCartCallback() {
                        @Override
                        public void onClearSuccess() {
                            panierManager.viderPanier();
                            films.clear();
                            adapter.notifyDataSetChanged();
                            tvNombreFilms.setText("0 film(s)");
                            Toast.makeText(PanierActivity.this, "Panier vidé", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onClearError(String errorMessage) {
                            Toast.makeText(PanierActivity.this, "Erreur: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }).execute();
                })
                .setNegativeButton("Non", null)
                .show();
    }

    // valider = passer tous les rentals du client de status_id 2 (panier) à 3 (location active)
    public void validerPanier(View view) {
        if (films.isEmpty()) {
            Toast.makeText(this, "Le panier est vide", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Valider le panier")
                .setMessage("Confirmer la validation de " + films.size() + " film(s) ?")
                .setPositiveButton("Valider", (dialog, which) -> {
                    TokenManager tokenManager = TokenManager.getInstance(this);
                    Integer customerId = tokenManager.getCustomerId();

                    if (customerId == null) {
                        Toast.makeText(this, "Erreur: Customer ID non trouvé", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    JSONObject requestBody = new JSONObject();
                    try {
                        requestBody.put("customerId", customerId);
                    } catch (JSONException e) {
                        Toast.makeText(this, "Erreur interne", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new CheckoutTask(this, requestBody, new CheckoutTask.CheckoutCallback() {
                        @Override
                        public void onCheckoutSuccess(int itemsCount) {
                            panierManager.viderPanier();
                            films.clear();
                            adapter.notifyDataSetChanged();
                            tvNombreFilms.setText("0 film(s)");
                            Toast.makeText(PanierActivity.this,
                                "Panier validé avec succès ! (" + itemsCount + " film(s))",
                                Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onCheckoutError(String errorMessage) {
                            // erreur serveur - on ne vide pas le sqlite pour ne pas perdre le panier
                            Toast.makeText(PanierActivity.this,
                                "Erreur: " + errorMessage,
                                Toast.LENGTH_LONG).show();
                        }
                    }).execute();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
}
