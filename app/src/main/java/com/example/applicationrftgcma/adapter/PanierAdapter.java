package com.example.applicationrftgcma.adapter;

// adaptateur pour la liste du panier (PanierActivity)
// le clic sur "supprimer" est délégué à PanierActivity via OnFilmSupprimerListener -
// c'est l'activité qui affiche la confirmation et met à jour la bdd, pas l'adapter

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.model.Film;

public class PanierAdapter extends ArrayAdapter<Film> {

    private Context context;
    private List<Film> films;
    private OnFilmSupprimerListener listener;

    // callback vers PanierActivity pour lui signaler qu'un film doit être supprimé
    public interface OnFilmSupprimerListener {
        void onFilmSupprimer(Film film, int position);
    }

    // le 0 dans super() indique qu'on ne fournit pas de layout par défaut - getView() le gère lui-même
    public PanierAdapter(Context context, List<Film> films, OnFilmSupprimerListener listener) {
        super(context, 0, films);
        this.context  = context;
        this.films    = films;
        this.listener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Film film = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_panier, parent, false);
        }

        TextView tvTitreFilmPanier = convertView.findViewById(R.id.tvTitreFilmPanier);
        TextView tvInfoFilmPanier  = convertView.findViewById(R.id.tvInfoFilmPanier);
        Button   btnSupprimerFilm  = convertView.findViewById(R.id.btnSupprimerFilm);

        if (film != null) {
            tvTitreFilmPanier.setText(film.getTitre());

            String infos = film.getAnnee() + " • " + film.getDureeFormatee() + " • " + film.getRating();
            tvInfoFilmPanier.setText(infos);

            // on notifie l'activité et c'est elle qui gère la suppression -
            // l'adapter n'a pas accès au sqlite ni à l'api
            btnSupprimerFilm.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFilmSupprimer(film, position);
                }
            });
        }

        return convertView;
    }
}
