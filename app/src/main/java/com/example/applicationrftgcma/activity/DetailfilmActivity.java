package com.example.applicationrftgcma.activity;

// écran de détail d'un film - reçoit le film sérialisé en json depuis ListefilmsActivity
// et délègue le parsing à DetailfilmTask pour ne pas bloquer le thread ui

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.manager.PanierManager;
import com.example.applicationrftgcma.manager.TokenManager;
import com.example.applicationrftgcma.model.Film;
import com.example.applicationrftgcma.task.AddToCartTask;
import com.example.applicationrftgcma.task.DetailfilmTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class DetailfilmActivity extends AppCompatActivity {

    private ProgressBar progressBar;

    // filmActuel est null jusqu'à ce que DetailfilmTask finisse le parsing -
    // le bouton panier vérifie cette valeur pour éviter un NullPointerException
    private Film filmActuel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailfilm);

        progressBar = findViewById(R.id.progressBarDetail);

        Intent intent = getIntent();
        String filmJson = intent.getStringExtra("filmJson");

        new DetailfilmTask(this).execute(filmJson);

        Button btnRetour = findViewById(R.id.btnRetour);
        btnRetour.setOnClickListener(v -> finish());

        Button btnAjouterPanier = findViewById(R.id.btnAjouterPanier);
        btnAjouterPanier.setOnClickListener(v -> {
            android.util.Log.d("DetailfilmActivity", ">>> Bouton Ajouter au panier cliqué");
            if (filmActuel != null) {
                TokenManager tokenManager = TokenManager.getInstance(this);
                Integer customerId = tokenManager.getCustomerId();

                android.util.Log.d("DetailfilmActivity", ">>> CustomerId: " + customerId + ", FilmId: " + filmActuel.getId());

                if (customerId == null) {
                    Toast.makeText(this, "Erreur: Vous devez être connecté", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JSONObject requestBody = new JSONObject();
                    try {
                        requestBody.put("customerId", customerId);
                        requestBody.put("filmId", filmActuel.getId());
                    } catch (JSONException e) {
                        Toast.makeText(this, "Erreur interne", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    android.util.Log.d("DetailfilmActivity", ">>> Avant création AddToCartTask");

                    // l'api crée un rental avec status_id = 2 (dans le panier)
                    // le rentalId retourné est stocké localement pour pouvoir supprimer le rental plus tard
                    new AddToCartTask(this, requestBody, new AddToCartTask.AddToCartCallback() {
                        @Override
                        public void onAddToCartSuccess(int rentalId) {
                            filmActuel.setRentalId(rentalId);

                            PanierManager panierManager = PanierManager.getInstance(DetailfilmActivity.this);
                            panierManager.ajouterFilm(filmActuel);

                            Toast.makeText(DetailfilmActivity.this,
                                "Film ajouté au panier",
                                Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onAddToCartError(String errorMessage) {
                            Toast.makeText(DetailfilmActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG).show();
                        }
                    }).execute();
                    android.util.Log.d("DetailfilmActivity", ">>> Après execute AddToCartTask");
                } catch (Exception e) {
                    android.util.Log.e("DetailfilmActivity", ">>> ERREUR lors de l'ajout au panier", e);
                    Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                android.util.Log.e("DetailfilmActivity", ">>> filmActuel est NULL!");
            }
        });
    }

    public void showProgressBar(boolean visible) {
        if (progressBar != null) {
            progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    // appelé par DetailfilmTask une fois le json parsé
    // si film == null le parsing a échoué (json invalide ou absent de l'intent)
    public void mettreAJourActivityAvecFilm(Film film) {
        if (film == null) {
            TextView tvTitreFilmDetail = findViewById(R.id.tvTitreFilmDetail);
            tvTitreFilmDetail.setText("Erreur : Impossible de charger le film");
            return;
        }

        // on mémorise le film pour que le bouton panier puisse l'utiliser
        this.filmActuel = film;

        TextView tvTitreFilmDetail = findViewById(R.id.tvTitreFilmDetail);
        TextView tvInfosFilm       = findViewById(R.id.tvInfosFilm);
        TextView tvDescription     = findViewById(R.id.tvDescription);
        TextView tvLangue          = findViewById(R.id.tvLangue);
        TextView tvRealisateurs    = findViewById(R.id.tvRealisateurs);
        TextView tvActeurs         = findViewById(R.id.tvActeurs);
        TextView tvCategories      = findViewById(R.id.tvCategories);

        tvTitreFilmDetail.setText(film.getTitre());

        String infos = film.getAnnee() + " • " + film.getDureeFormatee() + " • " + film.getRating();
        tvInfosFilm.setText(infos);

        tvDescription.setText(film.getDescription());
        tvLangue.setText(film.getLangue());

        StringBuilder realisateurs = new StringBuilder();
        List<Film.Director> directors = film.getDirectors();
        if (directors != null && !directors.isEmpty()) {
            for (int i = 0; i < directors.size(); i++) {
                realisateurs.append("• ").append(directors.get(i).getFullName());
                if (i < directors.size() - 1) {
                    realisateurs.append("\n");
                }
            }
            tvRealisateurs.setText(realisateurs.toString());
        } else {
            tvRealisateurs.setText("Aucun réalisateur");
        }

        StringBuilder acteurs = new StringBuilder();
        List<Film.Actor> actors = film.getActors();
        if (actors != null && !actors.isEmpty()) {
            for (int i = 0; i < actors.size(); i++) {
                acteurs.append("• ").append(actors.get(i).getFullName());
                if (i < actors.size() - 1) {
                    acteurs.append("\n");
                }
            }
            tvActeurs.setText(acteurs.toString());
        } else {
            tvActeurs.setText("Aucun acteur");
        }

        StringBuilder categoriesBuilder = new StringBuilder();
        List<Film.Category> cats = film.getCategories();
        if (cats != null && !cats.isEmpty()) {
            for (int i = 0; i < cats.size(); i++) {
                categoriesBuilder.append("• ").append(cats.get(i).getName());
                if (i < cats.size() - 1) {
                    categoriesBuilder.append("\n");
                }
            }
            tvCategories.setText(categoriesBuilder.toString());
        } else {
            tvCategories.setText("Aucune catégorie");
        }
    }
}
