# Luigi - Application mobile de réservation de DVD

Application Android du projet **RFTG (Raise From The Graveyard)**.

Luigi permet à un client de parcourir un catalogue de films, de les ajouter à son panier et de valider sa location.

---

## Le projet RFTG

RFTG est un système de location de DVD composé de trois applications :

| Application | Technologie | Description |
|-------------|-------------|-------------|
| **Luigi** (ce repo) | Android (Java) | Application mobile - réservation de DVD |
| **Mario** | Laravel (Web) | Interface d'administration des réservations |
| **Toad** | API REST | Backend consommé par Luigi et Mario |

### L'API REST Toad

Toad est l'API REST qui fait le lien entre les applications et la base de données.

- **Base de données** : MySQL, nommée **Peach** - modèle basé sur [Sakila](https://dev.mysql.com/doc/sakila/en/) modifié pour le projet RFTG
- **Consommateurs** : Luigi (mobile Android) et Mario (web Laravel)
- **Authentification** : JWT dans le header `Authorization: Bearer <token>`

> Liens des dépôts :
> - Toad (API REST + dump BDD Peach) : *(lien à compléter)*
> - Mario (Admin Web Laravel) : *(lien à compléter)*

---

## Prérequis

- Android Studio (Hedgehog ou supérieur recommandé)
- JDK 8 ou supérieur
- Un appareil Android (API 23+) ou un émulateur AVD
- L'API Toad lancée et accessible depuis le réseau de l'appareil/émulateur

---

## Installation

### 1. Cloner le dépôt

```bash
git clone <url-du-repo>
cd CMA_RFTG_Luigi
```

### 2. Ouvrir dans Android Studio

`File > Open` → sélectionner le dossier `CMA_RFTG_Luigi`

Android Studio détecte automatiquement le projet Gradle et télécharge les dépendances.

### 3. Configurer l'URL de l'API

L'URL de l'API peut être configurée de deux façons :

**Via `res/values/arrays.xml`** (pour ajouter une URL prédéfinie dans le spinner) :
```xml
<string-array name="listeURLs">
    <item>http://10.0.2.2:8180</item>   <!-- émulateur -> localhost -->
    <item>http://rftg.mtb111.com</item>  <!-- serveur de prod -->
</string-array>
```

**Via l'écran de connexion** : le champ URL est modifiable directement dans l'application au lancement.

> `10.0.2.2` est l'adresse spéciale de l'émulateur Android pour atteindre `localhost` de la machine hôte.
> Sur un appareil physique, utiliser l'IP locale de la machine hébergeant l'API (ex: `192.168.1.X`).

### 4. Configurer le token JWT

Copier le fichier exemple et renseigner le token fourni avec l'API Toad :

```bash
cp app/src/main/res/values/strings.xml.example app/src/main/res/values/strings.xml
```

Ouvrir `strings.xml` et remplacer `VOTRE_TOKEN_JWT_ICI` par le vrai token.

> `strings.xml` est dans le `.gitignore` - il ne sera jamais commité.

### 5. Autoriser le trafic HTTP (si nécessaire)

Le fichier `res/xml/network_security_config.xml` est déjà configuré pour autoriser les connexions HTTP (non HTTPS), nécessaire pour les environnements de développement.

---

## Lancer le projet

### Sur émulateur

1. Dans Android Studio, créer un AVD via `Device Manager` (API 23 minimum recommandée)
2. Sélectionner le device dans la barre d'outils
3. Cliquer sur **Run** (Shift+F10)

### Sur appareil physique

1. Activer le **mode développeur** sur l'appareil (`Paramètres > À propos > Numéro de build x7`)
2. Activer le **débogage USB**
3. Brancher l'appareil et l'autoriser
4. Le sélectionner dans la barre d'outils Android Studio
5. Cliquer sur **Run**

### Générer un APK

```
Build > Build Bundle(s) / APK(s) > Build APK(s)
```

L'APK se trouve dans `app/build/outputs/apk/debug/app-debug.apk`.

---

## Connexion à l'API Toad

L'application communique avec Toad via HTTP REST. Toutes les requêtes incluent un token JWT dans le header :

```
Authorization: Bearer <token>
```

Le token est stocké dans `res/values/strings.xml` (clé `api_jwt_token`).

### Endpoints utilisés

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/customers/verify` | Authentification (retourne le customerId) |
| `GET` | `/films` | Liste de tous les films disponibles |
| `POST` | `/cart/add` | Ajouter un film au panier (crée un rental status=2) |
| `DELETE` | `/cart/{rentalId}` | Supprimer un film du panier |
| `DELETE` | `/cart/clear/{customerId}` | Vider tout le panier |
| `POST` | `/cart/checkout` | Valider le panier (status 2 -> 3) |

### Mot de passe

Le mot de passe est hashé en **MD5** côté client avant envoi. C'est le format attendu par Toad.

---

## Structure du projet

```
app/src/main/java/com/example/applicationrftgcma/
├── activity/        # écrans (MainActivity, ListefilmsActivity, DetailfilmActivity, PanierActivity)
├── adapter/         # adaptateurs ListView (FilmAdapter, PanierAdapter)
├── helper/          # accès SQLite (DatabaseHelper)
├── manager/         # singletons utilitaires (TokenManager, PanierManager, UrlManager)
├── model/           # modèle de données (Film)
└── task/            # tâches réseau AsyncTask (Login, Listefilms, AddToCart...)
```

---

## Dépendances principales

| Librairie | Usage |
|-----------|-------|
| `Gson 2.10.1` | Désérialisation JSON -> objets Film |
| `AppCompat 1.7.1` | Composants UI compatibles |
| `ConstraintLayout 2.2.1` | Mise en page responsive |

---

## Notes de développement

- Le panier est persisté en **SQLite local** (`panier.db`) pour survivre aux rotations d'écran et aux changements d'activité
- Les opérations réseau sont toutes faites via `AsyncTask` (déprécié depuis API 30 mais fonctionnel)
- Le SQLite est toujours mis à jour **après** confirmation du serveur pour éviter les désynchronisations
- Les credentials de test pré-remplis (`cma@cma.com` / `password`) sont à retirer avant une mise en production
