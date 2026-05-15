package com.example.applicationrftgcma.adapter;

// adaptateur pour la liste de tous les films disponibles (ListefilmsActivity)
// chaque item a un bouton "ajouter au panier" qui appelle l'api puis enregistre en sqlite

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.model.Film;
import com.example.applicationrftgcma.manager.TokenManager;
import com.example.applicationrftgcma.manager.PanierManager;
import com.example.applicationrftgcma.task.AddToCartTask;

import org.json.JSONException;
import org.json.JSONObject;

public class FilmAdapter extends ArrayAdapter<Film> {

    private Context context;
    private List<Film> films;

    public FilmAdapter(Context context, List<Film> films) {
        super(context, R.layout.item_film, films);
        this.context = context;
        this.films = films;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Film film = films.get(position);

        // on réutilise la vue si elle a déjà été créée pour éviter les allocations répétées lors du scroll
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.item_film, parent, false);
        }

        TextView tvTitre = convertView.findViewById(R.id.tvTitreFilm);
        TextView tvRealisateur = convertView.findViewById(R.id.tvRealisateurFilm);
        TextView tvAnnee = convertView.findViewById(R.id.tvAnneeFilm);
        Button btnAjouterPanier = convertView.findViewById(R.id.btnAjouterPanierItem);

        tvTitre.setText(film.getTitre());
        tvRealisateur.setText(film.getRealisateur());
        tvAnnee.setText(String.valueOf(film.getAnnee()));

        btnAjouterPanier.setOnClickListener(v -> {
            android.util.Log.d("FilmAdapter", ">>> Bouton Ajouter au panier cliqué pour: " + film.getTitre());

            TokenManager tokenManager = TokenManager.getInstance(context);
            Integer customerId = tokenManager.getCustomerId();

            android.util.Log.d("FilmAdapter", ">>> CustomerId: " + customerId + ", FilmId: " + film.getId());

            if (customerId == null) {
                Toast.makeText(context, "Erreur: Vous devez être connecté", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject requestBody = new JSONObject();
            try {
                requestBody.put("customerId", customerId);
                requestBody.put("filmId", film.getId());
            } catch (JSONException e) {
                Toast.makeText(this.getContext(), "Erreur interne", Toast.LENGTH_SHORT).show();
                return;
            }

            // l'api crée un rental - le rentalId retourné est stocké sur le film
            // avant l'insertion en sqlite pour pouvoir le supprimer plus tard via DELETE /cart/{rentalId}
            new AddToCartTask(context, requestBody, new AddToCartTask.AddToCartCallback() {
                @Override
                public void onAddToCartSuccess(int rentalId) {
                    film.setRentalId(rentalId);

                    PanierManager panierManager = PanierManager.getInstance(context);
                    panierManager.ajouterFilm(film);

                    Toast.makeText(context, "Film ajouté au panier", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAddToCartError(String errorMessage) {
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
                }
            }).execute();
            android.util.Log.d("FilmAdapter", ">>> Après execute AddToCartTask");
        });

        // sans ça, le bouton capture le focus et bloque le setOnItemClickListener de la listview
        btnAjouterPanier.setFocusable(false);
        btnAjouterPanier.setFocusableInTouchMode(false);

        return convertView;
    }
}
