package com.example.applicationrftgcma.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.applicationrftgcma.R;
import com.example.applicationrftgcma.adapter.FilmAdapter;
import com.example.applicationrftgcma.model.Film;
import com.example.applicationrftgcma.task.ListefilmsTask;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// écran principal - liste tous les films disponibles à la location
// tousLesFilms conserve la liste complète reçue de l'api, filmsAffiches est le sous-ensemble
// affiché après filtres - cette séparation évite de rappeler l'api à chaque réinitialisation
public class ListefilmsActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private View loadingOverlay;
    private String listeFilmsResultat = "";
    private ListView listeFilms;
    private ConstraintLayout filterPanel;
    private boolean isFilterVisible = false;
    private List<Film> tousLesFilms = new ArrayList<>();
    private List<Film> filmsAffiches = new ArrayList<>();
    private FilmAdapter adapter;
    private EditText searchBar;
    private Spinner spinnerCategorie;
    private Spinner spinnerAnnee;
    private String categorieSelectionnee = "Toutes";
    private String anneeSelectionnee = "Toutes";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listefilms);

        listeFilms = findViewById(R.id.listeFilms);
        progressBar = findViewById(R.id.progressBar);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        filterPanel = findViewById(R.id.filterPanel);
        searchBar = findViewById(R.id.searchBar);
        spinnerCategorie = findViewById(R.id.spinnerCategorie);
        spinnerAnnee = findViewById(R.id.spinnerAnnee);
        Button btnFiltre = findViewById(R.id.btnFiltre);
        Button btnResetFilters = findViewById(R.id.btnResetFilters);

        // workaround android : sans ça, le champ de recherche capture le focus au lancement
        // et le clavier s'ouvre automatiquement en cachant la liste
        searchBar.setOnTouchListener((v, event) -> {
            searchBar.setFocusable(true);
            searchBar.setFocusableInTouchMode(true);
            return false;
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrerFilms();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnFiltre.setOnClickListener(v -> toggleFilterPanel());
        btnResetFilters.setOnClickListener(v -> reinitialiserFiltres());

        // l'url est construite dans la task via UrlManager, donc execute() n'a pas besoin de paramètre
        new ListefilmsTask(this).execute();
    }

    private void toggleFilterPanel() {
        if (isFilterVisible) {
            filterPanel.setVisibility(View.GONE);
            isFilterVisible = false;
        } else {
            filterPanel.setVisibility(View.VISIBLE);
            isFilterVisible = true;
        }
    }

    public void showProgressBar(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        loadingOverlay.setVisibility(visibility);
        progressBar.setVisibility(visibility);
    }

    public void mettreAJourActivityApresAppelRest(String resultatAppelRest) {
        listeFilmsResultat = resultatAppelRest;
        Log.d("mydebug", ">>>Pour ListefilmsActivity - mettreAJourActivityApresAppelRest=" + listeFilmsResultat);
        afficherListeFilms(listeFilmsResultat);
    }

    public void afficherListeFilms(String resultat) {
        TextView tvHeader = findViewById(R.id.tvTitre);

        if (resultat == null || resultat.isEmpty()) {
            tvHeader.setText("Aucun résultat reçu");
            Toast.makeText(this, "Aucune donnée reçue de l'API", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // TypeToken est nécessaire à cause de l'effacement de type java (type erasure) -
            // gson ne peut pas déduire List<Film> à l'exécution sans ce contournement
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Film>>(){}.getType();
            List<Film> films = gson.fromJson(resultat, listType);

            if (films != null && !films.isEmpty()) {
                tousLesFilms = new ArrayList<>(films);
                filmsAffiches = new ArrayList<>(films);

                tvHeader.setText("Liste des films");

                adapter = new FilmAdapter(this, filmsAffiches);
                listeFilms.setAdapter(adapter);

                listeFilms.setOnItemClickListener((parent, view, position, id) -> {
                    Film filmSelectionne = filmsAffiches.get(position);
                    ouvrirPageDetailfilm(filmSelectionne);
                });

                // les spinners sont peuplés après le chargement car les valeurs
                // viennent des films eux-mêmes, pas d'une liste statique
                initialiserSpinners();

                Log.d("mydebug", ">>>Films affichés avec succès : " + films.size() + " films");
            } else {
                tvHeader.setText("Aucun film trouvé");
                Toast.makeText(this, "La liste des films est vide", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            tvHeader.setText("Erreur lors du parsing des données");
            Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("mydebug", ">>>Erreur parsing JSON : " + e.getMessage());
        }
    }

    private void filtrerFilms() {
        String query = searchBar.getText().toString().toLowerCase().trim();

        List<Film> filmsTemporaires = new ArrayList<>();

        for (Film film : tousLesFilms) {
            boolean inclure = true;

            if (!query.isEmpty()) {
                boolean correspondRecherche = film.getTitre().toLowerCase().contains(query) ||
                        film.getRealisateur().toLowerCase().contains(query);
                if (!correspondRecherche) {
                    inclure = false;
                }
            }

            if (inclure && !categorieSelectionnee.equals("Toutes")) {
                boolean correspondCategorie = false;
                if (film.getCategories() != null) {
                    for (Film.Category cat : film.getCategories()) {
                        if (cat.getName().equals(categorieSelectionnee)) {
                            correspondCategorie = true;
                            break;
                        }
                    }
                }
                if (!correspondCategorie) {
                    inclure = false;
                }
            }

            if (inclure && !anneeSelectionnee.equals("Toutes")) {
                int annee = Integer.parseInt(anneeSelectionnee);
                if (film.getAnnee() != annee) {
                    inclure = false;
                }
            }

            if (inclure) {
                filmsTemporaires.add(film);
            }
        }

        filmsAffiches = filmsTemporaires;
        if (adapter != null) {
            adapter.clear();
            adapter.addAll(filmsAffiches);
            adapter.notifyDataSetChanged();
        }

        TextView tvHeader = findViewById(R.id.tvTitre);
        tvHeader.setText("Films");
    }

    private void initialiserSpinners() {
        // hashset pour dédupliquer - plusieurs films peuvent partager la même catégorie
        Set<String> categories = new HashSet<>();
        categories.add("Toutes");
        for (Film film : tousLesFilms) {
            if (film.getCategories() != null) {
                for (Film.Category cat : film.getCategories()) {
                    categories.add(cat.getName());
                }
            }
        }
        List<String> listeCategories = new ArrayList<>(categories);
        java.util.Collections.sort(listeCategories);

        ArrayAdapter<String> adapterCategorie = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listeCategories);
        adapterCategorie.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategorie.setAdapter(adapterCategorie);

        spinnerCategorie.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                categorieSelectionnee = parent.getItemAtPosition(position).toString();
                filtrerFilms();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        Set<String> annees = new HashSet<>();
        annees.add("Toutes");
        for (Film film : tousLesFilms) {
            annees.add(String.valueOf(film.getAnnee()));
        }
        List<String> listeAnnees = new ArrayList<>(annees);
        // "toutes" doit rester en tête - les années sont ensuite triées du plus récent au plus ancien
        java.util.Collections.sort(listeAnnees, new java.util.Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                if (a.equals("Toutes")) return -1;
                if (b.equals("Toutes")) return 1;
                return b.compareTo(a);
            }
        });

        ArrayAdapter<String> adapterAnnee = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listeAnnees);
        adapterAnnee.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAnnee.setAdapter(adapterAnnee);

        spinnerAnnee.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                anneeSelectionnee = parent.getItemAtPosition(position).toString();
                filtrerFilms();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void reinitialiserFiltres() {
        searchBar.setText("");
        // "toutes" est toujours en position 0 grâce au tri dans initialiserSpinners()
        spinnerCategorie.setSelection(0);
        spinnerAnnee.setSelection(0);
        categorieSelectionnee = "Toutes";
        anneeSelectionnee = "Toutes";
        filtrerFilms();
    }

    public void quitterApplication(View view) {
        finishAffinity();
    }

    public void ouvrirPagePanier(View view) {
        Intent intent = new Intent(this, PanierActivity.class);
        startActivity(intent);
    }

    // le film est sérialisé en json pour le passer via l'intent
    // car Film n'implémente pas Parcelable - gson est déjà présent donc c'est la solution la plus simple
    public void ouvrirPageDetailfilm(Film film) {
        Intent intent = new Intent(this, DetailfilmActivity.class);
        Gson gson = new Gson();
        String filmJson = gson.toJson(film);
        intent.putExtra("filmJson", filmJson);
        startActivity(intent);
    }
}
