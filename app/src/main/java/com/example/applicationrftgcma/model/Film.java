package com.example.applicationrftgcma.model;

// modèle de données représentant un film
// les annotations @SerializedName font le lien entre les clés json de l'api (anglais)
// et les noms de champs java (français) - gson s'en sert lors de la désérialisation
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Film {

    // constructeur vide requis par gson pour instancier l'objet lors de la désérialisation
    public Film() {
    }

    @SerializedName("filmId")
    private int id;

    @SerializedName("title")
    private String titre;

    @SerializedName("description")
    private String description;

    @SerializedName("releaseYear")
    private int annee;

    // Integer plutôt que int pour supporter null si la clé est absente du json
    @SerializedName("originalLanguageId")
    private Integer languageId;

    @SerializedName("length")
    private int duree;

    @SerializedName("rating")
    private String rating;

    @SerializedName("rentalRate")
    private double prix;

    @SerializedName("directors")
    private List<Director> directors;

    @SerializedName("actors")
    private List<Actor> actors;

    @SerializedName("categories")
    private List<Category> categories;

    // rentalId n'est pas dans le json /films - il est renseigné après un POST /cart/add
    // il est stocké en sqlite pour pouvoir appeler DELETE /cart/{rentalId} lors de la suppression
    private int rentalId;

    // réalisateur mappé depuis "directors": [{ "firstName": "...", "lastName": "..." }]
    public static class Director {
        @SerializedName("firstName")
        private String firstName;

        @SerializedName("lastName")
        private String lastName;

        public String getFullName() {
            return firstName + " " + lastName;
        }
    }

    // acteur mappé depuis "actors": [{ "actorId": 1, "firstName": "...", "lastName": "..." }]
    public static class Actor {
        @SerializedName("actorId")
        private int id;

        @SerializedName("firstName")
        private String firstName;

        @SerializedName("lastName")
        private String lastName;

        public String getFullName() {
            return firstName + " " + lastName;
        }
    }

    // catégorie mappée depuis "categories": [{ "categoryId": 1, "name": "Action" }]
    public static class Category {
        @SerializedName("categoryId")
        private int id;

        @SerializedName("name")
        private String name;

        public String getName() {
            return name;
        }
    }

    public int getId() {
        return id;
    }

    public String getTitre() {
        return titre != null ? titre : "";
    }

    public String getDescription() {
        return description != null ? description : "Aucune description disponible";
    }

    public int getAnnee() {
        return annee;
    }

    // mapping des ids de langue issus de la table language de la bdd sakila (modifiée pour rftg)
    // on retourne "anglais" par défaut car c'est la langue la plus courante dans sakila
    public String getLangue() {
        if (languageId == null) {
            return "Anglais";
        }
        switch (languageId) {
            case 1: return "Anglais";
            case 2: return "Italien";
            case 3: return "Japonais";
            case 4: return "Mandarin";
            case 5: return "Français";
            case 6: return "Allemand";
            default: return "Anglais";
        }
    }

    // formate la durée brute en minutes en chaîne lisible
    // exemples : 114 -> "1h54", 45 -> "45 min", 120 -> "2h"
    // le format("%02d") sert à afficher "1h05" et non "1h5"
    public String getDureeFormatee() {
        int heures = duree / 60;
        int minutes = duree % 60;
        if (heures > 0) {
            return heures + "h" + (minutes > 0 ? String.format("%02d", minutes) : "");
        } else {
            return minutes + " min";
        }
    }

    public String getRating() {
        return rating != null ? rating : "Non classé";
    }

    // retourne uniquement le premier réalisateur pour l'affichage en liste
    // (la page de détail affiche tous les réalisateurs)
    public String getRealisateur() {
        if (directors != null && !directors.isEmpty()) {
            return directors.get(0).getFullName();
        }
        return "Réalisateur inconnu";
    }

    public List<Director> getDirectors() {
        return directors;
    }

    public List<Actor> getActors() {
        return actors;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public double getPrix() {
        return prix;
    }

    public int getDuree() {
        return duree;
    }

    // alias pour DatabaseHelper - même valeur que getRating()
    public String getClassification() {
        return rating;
    }

    // alias pour DatabaseHelper - retourne le nom de langue lisible
    public String getLangueOriginaleName() {
        return getLangue();
    }

    // --- sérialisation pour sqlite ---
    // sqlite ne supporte pas les listes d'objets - on les aplatit en csv
    // ces strings sont stockées en base mais ne sont pas rechargées à la lecture,
    // car PanierActivity n'a besoin que des infos de base (titre, année, durée...)

    public String getRealisateursString() {
        if (directors == null || directors.isEmpty()) {
            return "Aucun réalisateur";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < directors.size(); i++) {
            sb.append(directors.get(i).getFullName());
            if (i < directors.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public String getActeursString() {
        if (actors == null || actors.isEmpty()) {
            return "Aucun acteur";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < actors.size(); i++) {
            sb.append(actors.get(i).getFullName());
            if (i < actors.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public String getCategoriesString() {
        if (categories == null || categories.isEmpty()) {
            return "Aucune catégorie";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < categories.size(); i++) {
            sb.append(categories.get(i).getName());
            if (i < categories.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    // --- setters utilisés par DatabaseHelper pour reconstruire un Film depuis sqlite ---

    public void setId(int id) {
        this.id = id;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public void setPrix(double prix) {
        this.prix = prix;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAnnee(int annee) {
        this.annee = annee;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }

    public void setClassification(String classification) {
        this.rating = classification;
    }

    public int getRentalId() {
        return rentalId;
    }

    // appelé dans onAddToCartSuccess() juste avant l'insertion en sqlite
    public void setRentalId(int rentalId) {
        this.rentalId = rentalId;
    }

    @Override
    public String toString() {
        return titre + " - " + getRealisateur() + " (" + prix + "€)";
    }
}
