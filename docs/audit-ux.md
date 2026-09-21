# Audit UX — Messous (Android / Compose)

Audit réalisé par lecture exhaustive du code UI (`app/src/main/java/com/budgetflow/app/ui`, 7 400 lignes
Compose, 16 écrans), des chaînes (`res/values/strings.xml`) et du moteur de calcul (`core-engine`).
Pas d'exécution sur appareil : les constats portent sur ce que le code produit à l'écran, les parcours,
les états manquants et l'accessibilité. Chaque constat est référencé `fichier:ligne`.

La promesse du produit, telle qu'écrite dans le code lui-même :
> « combien puis-je dépenser sans mettre mon mois en danger ? » — en cinq secondes, avant tout détail
> secondaire. (`ui/liberty/LibertyScreen.kt:70`)

C'est le fil conducteur de l'audit : tout ce qui éloigne l'utilisateur de cette réponse en 5 secondes
est un défaut.

---

## 1. Synthèse

**Ce qui est solide**
- L'architecture UX est bonne : 4 espaces (Liberté / Futur / Et si… / Moi), un moteur de calcul pur
  et testé, séparation claire plan vs solde réel.
- Les états de liberté ne reposent jamais sur la couleur seule : icône + libellé + valeur chiffrée
  (`ui/components/FreedomVisuals.kt:57`). Bon réflexe d'accessibilité.
- Les écrans de saisie montrent l'impact d'une dépense **avant** l'enregistrement
  (`budget_impact_expense`) : c'est la meilleure idée du produit.
- Reconnaissance de service pendant la frappe, extraction du montant depuis le texte, thème dynamique,
  export/import local, tout hors ligne : autant de bons choix.

**Les 5 problèmes qui coûtent le plus cher**

| # | Problème | Impact |
|---|---|---|
| P1 | L'écran d'accueil affiche **7 montants concurrents**, dont deux « par jour » contradictoires | La promesse des 5 secondes est perdue |
| P2 | Le chiffre héros (`reste à vivre`, plan) est **coloré par un calcul différent** (`argent libre`, solde) | Perte de confiance : un chiffre vert peut cacher une alerte |
| P3 | **Écrans sans retour** : Budget, Transactions, Statistiques n'ont ni flèche retour ni barre d'onglets | L'utilisateur est piégé (geste système uniquement) |
| P4 | « Enregistrer » **ne fait rien** en cas de saisie invalide, sans message | Échec silencieux, le pire des feedbacks |
| P5 | Le **seuil de sécurité vaut 0 par défaut** et n'est jamais proposé | Tout le discours « sécurité » du produit tourne à vide |

**Trois chantiers, par ordre de rentabilité**
1. **Hiérarchiser l'accueil** (P1, P2) — réduire à 1 chiffre + 1 phrase + 1 jauge.
2. **Réparer les fondamentaux d'interaction** (P3, P4, confirmations de suppression, accessibilité) —
   corrections courtes, gain immédiat.
3. **Finir le récit de sécurité** (P5, solde initial, mode allocation) — l'app promet une sécurité
   qu'elle ne configure jamais.

---

## 2. P1 — L'accueil répond à sept questions au lieu d'une

`ui/liberty/LibertyScreen.kt:160-330`

Dans l'ordre d'apparition, l'utilisateur reçoit :

1. Badge d'état (Confort / Attention / Sécurité)
2. **Reste à vivre** (héros, `displayMedium`) — base plan
3. « sur X prévus ce mois-ci » — budget disponible
4. « Argent libre (solde réel) : Y » — base solde
5. « Solde : Z » — solde bancaire brut
6. Carte **Reste à vivre au jour d'aujourd'hui** + curseur de jour → un 4ᵉ montant
7. Carte **« Tu peux dépenser environ N €/jour »** (`displaySmall`, presque aussi gros que le héros)
8. Jauge de seuil de sécurité (2 montants de plus)
9. Une phrase contextuelle qui cite **encore** un montant par jour

Sept à neuf montants, dont trois typographies « héros ». Aucun n'est hiérarchisé par rapport aux autres
autrement que par la taille, et deux d'entre eux se contredisent (voir P2).

**Contradiction la plus visible** : la carte « Tu peux dépenser environ » affiche
`summary.dailyRecommendedBudget` = `remainingToSpend / jours restants` (base **plan**,
`LibertyScreen.kt:278`), tandis que la phrase juste en dessous affiche `summary.freedomPerDay`
= `freeMoney / jours restants` (base **solde**, `LibertyScreen.kt:442`). Deux nombres différents,
même unité, même écran, à 100 pixels d'écart, présentés tous deux comme « ce que tu peux dépenser
par jour ».

### Proposition

Un accueil en trois zones, le reste déplacé d'un cran.

```
┌──────────────────────────────────────┐
│  ● Confort                           │   badge état
│                                      │
│  Il te reste                         │
│        1 240 €                       │   UN chiffre héros
│  soit 52 € par jour, 24 jours        │   UNE déclinaison, dérivée du même calcul
│                                      │
│  ▸ D'où vient ce chiffre ?           │   dépliant : revenus, fixes, enveloppes, épargne, solde
├──────────────────────────────────────┤
│  ▓▓▓▓▓▓▓▓▓▓░│░░░░   marge 340 €      │   jauge seuil (si seuil configuré)
├──────────────────────────────────────┤
│  Une grosse dépense arrive jeudi.    │   UNE phrase
│  [ Simuler une dépense ]             │
├──────────────────────────────────────┤
│  À venir · Fixes · Transactions      │   listes, inchangées
└──────────────────────────────────────┘
```

Règles à tenir :
- **Un seul « par jour »** dans toute l'app. Choisir `freedomPerDay` (base solde, le plus proche du
  réel) et supprimer l'autre de l'accueil, ou l'inverse — mais un seul.
- Le **détail du calcul** (budget prévu, solde, argent libre) passe dans un bloc dépliable
  « D'où vient ce chiffre ? ». Il répond à une question de confiance, pas à la question des
  5 secondes ; il ne mérite pas la première vue.
- La carte « par jour » perd son `displaySmall` : elle devient une ligne sous le héros.
- Le **curseur de jour** (`LibertyScreen.kt:222-263`) est à déplacer dans « Mon futur », qui est
  littéralement l'écran du voyage dans le temps. Deux curseurs temporels dans deux écrans différents,
  qui ne montrent pas la même grandeur, c'est une redondance coûteuse.
- Au passage, son libellé est faux : la carte s'intitule « Reste à vivre **au jour d'aujourd'hui** »
  même quand le curseur est positionné au 28. Le libellé doit suivre le jour sélectionné.

---

## 3. P2 — Le chiffre héros est coloré par un autre calcul

`ui/liberty/LibertyScreen.kt:178-189`

```kotlin
FreedomStateBadge(freedomState)        // freedomState = f(freeMoney − seuil)      → base SOLDE
AnimatedMoneyText(
    amount = summary.remainingToSpend, //                                          → base PLAN
    color  = freedomState.color()      // couleur issue du calcul solde
)
```

`freedomState` est calculé dans `BudgetEngine.summarizeMonth` à partir de `freeMoney` et du seuil
(`core-engine/.../BudgetEngine.kt:64-66`) ; `remainingToSpend` est une projection de plan qui ignore
totalement le solde bancaire. Le commentaire de `MonthSummary` l'écrit noir sur blanc :

> `availableBudget` et `reallyAvailableNow` sont délibérément deux nombres différents et **ne doivent
> jamais être présentés comme interchangeables**. (`core-engine/.../model/MonthSummary.kt:7-8`)

L'écran d'accueil fait exactement ce que le moteur interdit : il habille l'un avec la couleur de l'autre.
Cas concret : plan sain (+800 € de reste à vivre) mais compte à sec avant le virement du 5 → le
1 200 € s'affiche en **rouge** ; ou l'inverse, un découvert annoncé en **vert**. Dans une app dont tout
l'argument est la confiance dans un chiffre unique, c'est le défaut le plus grave de l'audit.

### Proposition
- Choisir **une** base pour le chiffre héros et sa couleur. Recommandation : `freeMoney` (solde réel),
  car c'est le seul nombre que l'utilisateur peut recouper avec son appli bancaire.
- Quand `freeMoney` est `null` (aucun compte), afficher le chiffre plan **en couleur neutre**, plus
  une invitation « ajouter mon solde pour un chiffre réel ».
- Ajouter un test unitaire de garde : couleur et montant proviennent du même champ de `MonthSummary`.

---

## 4. P3 — Trois écrans sans sortie

| Écran | Barre d'onglets | Flèche retour |
|---|---|---|
| Budget (`ui/budget/BudgetScreen.kt:88`) | non (route secondaire) | **non** |
| Transactions (`ui/transactions/TransactionsScreen.kt:55`) | non | **non** |
| Statistiques (`ui/statistics/StatisticsScreen.kt:48`) | non | **non** |

`BudgetFlowNavHost.kt:38` masque la barre du bas dès qu'on n'est pas sur une des quatre routes
principales ; ces trois écrans n'ajoutent pas de `navigationIcon`. Comptes et Catégories le font
correctement (`AccountsScreen.kt:63`) — c'est un oubli, pas un choix.

Le chemin le plus probable pour un nouvel utilisateur mène droit dans le piège : accueil vide →
« Ajouter un revenu » → Budget (`LibertyScreen.kt:142`) → aucune sortie visible.

Aggravant : ce bouton de l'état vide ouvre **Budget** au lieu d'ouvrir directement le formulaire
« Ajouter un revenu », alors que le libellé du bouton est « Ajouter un revenu ».

### Proposition
- `navigationIcon = { IconButton(onBack) { Icon(ArrowBack, contentDescription = "Retour") } }` sur les
  trois écrans (correctif de 6 lignes).
- L'état vide de l'accueil pointe vers `Routes.ADD_INCOME`, pas vers `Routes.BUDGET`.
- Vérifier chaque `TopAppBar` de route secondaire : l'invariant « pas de barre d'onglets ⇒ flèche
  retour » mérite un test d'instrumentation.

---

## 5. P4 — Les échecs silencieux

`ui/transactions/AddEditTransactionViewModel.kt:126-130`, `ui/budget/AddEditIncomeViewModel.kt:106-109`,
`ui/budget/AddEditExpenseViewModel.kt:126-129`

```kotlin
fun save() {
    val amount = state.amount.toAmountOrNull() ?: return   // rien ne se passe
    val accountId = state.accountId ?: return              // rien ne se passe
    if (amount <= 0.0) return                              // rien ne se passe
    …
}
```

Le bouton « Enregistrer » est toujours actif (`AddEditTransactionScreen.kt:145`). L'utilisateur appuie,
l'écran ne bouge pas, aucun message. C'est le scénario que toute heuristique d'utilisabilité classe en
priorité maximale : une action sans retour.

Même famille de défauts ailleurs :
- **Suppressions sans confirmation ni annulation** : transaction (`TransactionsScreen.kt:104`), revenu,
  dépense fixe, enveloppe, objectif (`BudgetScreen.kt:153-233`), et surtout **compte**
  (`AccountsScreen.kt:91`) — supprimer un compte emporte l'historique qui y est rattaché, en un seul
  tap, sans question.
- **Snackbars menteuses** dans Moi : après un export réussi, le message affiché est le libellé du
  bouton, « Exporter mes données » (`SettingsScreen.kt:85`) ; idem après import (`:231`). En cas
  d'échec d'import, le message est `exception.message` brut, donc technique et anglophone.
- **« Ajouter réellement cette dépense » désactivé sans explication** quand aucun compte n'existe
  (`WhatIfScreen.kt:173`) : un bouton gris sans raison.

### Proposition
- Un `formError: Int?` dans chaque état de formulaire ; `supportingText` rouge sur le champ fautif +
  bouton désactivé tant que la saisie n'est pas valide (désactivé **avec** un texte d'aide, jamais seul).
- Suppression = `Snackbar` avec action « Annuler » (5 s) pour les lignes ; `AlertDialog` nominatif pour
  un compte (« Supprimer « Compte courant » et ses 42 transactions ? »).
- Messages de confirmation dédiés : `settings_export_success`, `settings_import_success`,
  `settings_import_error`, et non le libellé de l'action.

---

## 6. P5 — Le seuil de sécurité : promesse tenue nulle part

Le seuil vaut **0 par défaut** (`data/prefs/UserPreferences.kt:60-61`) et rien dans l'onboarding ne le
propose (`ui/onboarding/OnboardingViewModel.kt` : revenu, dépenses fixes, enveloppes — c'est tout).

Conséquences en chaîne, pour la quasi-totalité des utilisateurs :
- La jauge « seuil de sécurité » n'affiche **ni repère ni marge** (`FreedomVisuals.kt:103`, conditionné
  à `safetyThreshold > 0`) : il reste une barre pleine sans signification.
- « Et si…? » affirme « **Ton seuil de sécurité serait respecté** » (`whatif_threshold_ok`) quelle que
  soit la dépense simulée — une réassurance fausse, produite par une comparaison à zéro.
- La section « Ma sécurité financière » de Moi parle d'un montant « toujours gardé de côté » qui vaut 0 €.

Même angle mort pour le **solde de départ** : l'onboarding crée un compte à 0 €
(`OnboardingViewModel.kt:133`) sans jamais demander le solde réel. Or `freeMoney`, la jauge, l'état de
liberté et tout « Mon futur » en dépendent. L'utilisateur termine l'onboarding sur un écran dont la
moitié des chiffres sont faux, sans savoir pourquoi.

### Proposition
- Ajouter **deux questions à l'onboarding** (une par étape, pas un formulaire) :
  « Combien as-tu sur ton compte aujourd'hui ? » et « Combien veux-tu ne jamais descendre en dessous ? »,
  chacune avec « Plus tard » explicite.
- Proposer un seuil **par défaut calculé** (ex. 7 jours de dépenses courantes) plutôt qu'une page blanche.
- Tant que le seuil vaut 0 : remplacer « Ton seuil de sécurité serait respecté » par une invitation à le
  définir, et masquer la jauge au lieu d'en montrer une version vide.

---

## 7. Accessibilité

### 7.1 Contrastes — deux échecs WCAG AA avérés

Les couleurs sémantiques sont des constantes fixes, identiques en clair et en sombre
(`ui/theme/Color.kt:25-28`), alors que les fonds, eux, s'inversent. Ratios mesurés :

| Couleur | Sur fond clair `#FBFDF9` | Sur fond sombre `#191C1A` |
|---|---|---|
| `PositiveGreen #2E7D5B` | 4.88 ✅ | **3.44 ❌** |
| `NegativeRed #BA1A1A` | 6.31 ✅ | **2.66 ❌** |
| `NeutralAmber #B8860B` | **3.18 ❌** | 5.28 ✅ |

Seuil AA pour du texte courant : 4.5:1. Concrètement, **en thème sombre, tous les montants négatifs
sont illisibles** (2.66:1) — c'est-à-dire exactement l'information qui compte le plus ; et en thème
clair, l'état « Attention » passe sous le seuil. Le problème touche aussi les montants colorés par
`colorBySign`, présents sur tous les écrans.

**Proposition** : dédoubler les trois couleurs sémantiques (`PositiveGreenLight/Dark`, etc.) et les
exposer via un `CompositionLocal` ou une extension du thème, à l'image de ce que fait déjà Material 3
pour `error`/`onErrorContainer`. Cibles : ≥ 4.5:1 dans les deux thèmes. Un test unitaire de ratio sur
les 6 valeurs empêche la régression.

### 7.2 Lecteurs d'écran

29 `contentDescription = null` contre 8 renseignés (`ui/`). Les nulls décoratifs sont légitimes, mais
plusieurs portent l'unique information de leur contrôle :
- Les `IconButton` de suppression de Budget (`BudgetScreen.kt:153, 185, 205, 233`) et de l'onboarding
  (`OnboardingScreen.kt:250`) : TalkBack annonce « bouton », sans dire quoi ni sur quoi.
- Les flèches retour de tous les formulaires (`AddEditTransactionScreen.kt:80`,
  `AddEditExpenseScreen.kt:72`, `AccountsScreen.kt:63`) : idem.
- La jauge de seuil est un `Canvas` nu (`FreedomVisuals.kt:83`) : invisible pour TalkBack. Les valeurs
  textuelles adjacentes sauvent l'essentiel, mais un `contentDescription` global sur la jauge (« argent
  libre 1 240 €, seuil 900 €, marge 340 € ») serait plus juste.
- Les curseurs (accueil, futur, « Et si…? ») n'ont ni `contentDescription` ni `stateDescription` : un
  curseur qui annonce « 17 sur 31 » sans unité n'est pas exploitable.

Aucun `semantics {}` ni `testTag` dans tout le module UI : ni l'accessibilité ni les tests d'IHM n'ont
de prise. C'est aussi ce qui explique l'absence totale de tests d'instrumentation.

### 7.3 Divers
- `EmptyState` (`components/EmptyState.kt:26`) affiche son icône à la taille par défaut (24 dp) dans un
  bloc centré prévu pour une illustration : visuellement chétif. 48–64 dp + une action explicite.
- Les états vides de Budget réutilisent l'icône `Add` et le libellé du bouton comme titre
  (`BudgetScreen.kt:136`) : « Ajouter un revenu » comme phrase d'accueil d'une liste vide, sans bouton
  associé alors que le FAB, lui, existe.

---

## 8. Contenu, ton et langue

- **Tutoiement et vouvoiement cohabitent**, parfois à un écran d'écart :
  « Sachez à tout moment… », « Indiquez votre revenu » (onboarding) vs « Tu peux dépenser environ… »,
  « Ajoute un revenu » (accueil). Choisir — le tutoiement colle mieux au positionnement « ta liberté » —
  et repasser `strings.xml` en entier.
- **Libellé de bouton faux** : dans l'onboarding, le bouton « précédent » porte la chaîne
  `action_cancel` = « Annuler » (`OnboardingScreen.kt:107`). Un utilisateur qui veut revenir en arrière
  craint de tout perdre.
- **Chaînes en dur non traduisibles** : « · inactif » (`BudgetScreen.kt:146`), « Autre »
  (`StatisticsScreen.kt:91`), mois abrégés (`ScheduleFields.kt:131`), libellés de repli « Revenu »,
  « Dépense », « Enveloppe », « Objectif », « Compte courant ».
- **Locale figée** : `Locale.FRANCE` pour la monnaie (`MoneyText.kt:18`), `Locale.FRENCH` pour les dates
  (4 occurrences). Le champ `currency` des comptes est donc décoratif : un compte en CHF s'affiche en €.
  Soit on assume mono-devise (et on retire le champ de l'IHM), soit on formate par compte.
- **Centimes partout** : le chiffre héros affiche « 1 234,56 € ». Pour une lecture en 5 secondes,
  arrondir à l'euro sur les montants héros (et garder les centimes dans les listes et les formulaires).
- **Pas de pluriels** : `liberty_daily_freedom_until_end` produit « jusqu'à la fin du mois (1 j.) ».

---

## 9. Parcours et fonctionnalités inachevées

- **« Que faire de cette somme ? »** (`WhatIfScreen.kt:236-300`) : les quatre scénarios sont des
  pourcentages en dur (60/40, 100 %, 100 %), sans lien avec les objectifs réels de l'utilisateur, et
  surtout **sans aucune action** — on ne peut rien enregistrer. C'est une impasse : soit on la branche
  sur les objectifs d'épargne existants avec un bouton « Appliquer », soit on la retire de la v1.
- **Verrouillage biométrique** (`ui/lock/LockScreen.kt`) : `onAuthenticationError` n'est pas implémenté,
  et le réglage s'active sans vérifier `BiometricManager.canAuthenticate()`
  (`SettingsViewModel.kt:37`). Sur un appareil sans biométrie enrôlée, l'utilisateur peut s'enfermer
  hors de ses données sans message d'erreur. À corriger avant toute publication.
  Par ailleurs, l'écran exige un tap sur « Déverrouiller » avant d'afficher le prompt système : le
  déclencher automatiquement au premier affichage supprime une étape à chaque ouverture.
- **Titre d'écran d'édition incorrect** : modifier une transaction affiche « Ajouter une transaction »
  (`AddEditTransactionScreen.kt:79`), et la suppression n'y est pas accessible — il faut ressortir vers
  la liste. Les écrans revenu/dépense, eux, gèrent bien le cas (`AddEditExpenseScreen.kt:71`).
- **Aucune protection contre la perte de saisie** : quitter un formulaire rempli par la flèche retour
  ne demande rien.
- **Aucun écran de chargement** : `LibertyScreen.kt:133` affiche une `Box` vide pendant le calcul.
  Au premier lancement sur une base volumineuse (import), l'app paraît cassée. Un squelette ou un
  indicateur suffit.
- **« Mon futur » — curseur à 31 crans** (`FutureScreen.kt:163`) : sur un pouce, chaque jour fait ~10 dp.
  Préférer des paliers hebdomadaires + des repères de date sous la piste, ou une timeline défilante
  dont le jour actif se cale au centre.
- **Statistiques sans état vide** : sans transaction, les cartes s'affichent avec des 0 € et un
  graphique plat, plutôt qu'un message.

---

## 10. Backlog proposé

**Lot 1 — Confiance et fondamentaux** (correctifs courts, gros gain)
1. Aligner le chiffre héros et sa couleur sur une seule base de calcul (P2).
2. Flèche retour sur Budget / Transactions / Statistiques (P3).
3. Validation visible des formulaires : message + bouton désactivé (P4).
4. Confirmation ou annulation sur toutes les suppressions, dialogue nominatif pour un compte (P4).
5. Couleurs sémantiques déclinées clair/sombre, ≥ 4.5:1 (§7.1).
6. Garde-fou biométrie : `canAuthenticate()` avant activation + `onAuthenticationError` (§9).
7. Snackbars de confirmation dédiées, bouton « précédent » de l'onboarding renommé (§5, §8).

**Lot 2 — La promesse des 5 secondes**
8. Refonte de la hiérarchie de l'accueil : 1 chiffre, 1 déclinaison par jour, 1 phrase, 1 jauge (P1).
9. Détail du calcul dans un bloc « D'où vient ce chiffre ? ».
10. Curseur de jour déplacé vers « Mon futur », libellé synchronisé avec le jour choisi.
11. Montants héros arrondis à l'euro.

**Lot 3 — Le récit de sécurité**
12. Onboarding : solde actuel + seuil de sécurité (avec valeur par défaut suggérée).
13. Messages « Et si…? » corrects quand le seuil vaut 0.
14. « Que faire de cette somme ? » branché sur les objectifs, ou retiré de la v1.

**Lot 4 — Finition**
15. `contentDescription` et `semantics` sur les contrôles porteurs d'information ; premiers tests d'IHM.
16. Passage complet de `strings.xml` : tutoiement unique, pluriels, chaînes en dur extraites.
17. États vides avec action (Statistiques, Budget), état de chargement de l'accueil.
18. Politique de devise assumée (mono-devise ou formatage par compte).
