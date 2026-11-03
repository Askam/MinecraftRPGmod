# Handoff 2b — complément serveur

Ajout à `HANDOFF-2-cloud.md`. Même session cloud, même règles : rien de
visuel, aucun client de jeu, et on ne prétend pas avoir testé en jeu.

Ces trois chantiers sont entièrement côté serveur. L'écran d'administration,
lui, est du client et relève du handoff 3 — mais l'état qu'il manipule et la
commande qui l'ouvre se construisent ici.

## Où en est le code (à lire avant de commencer)

- **Des commandes de test existent déjà** sous `/mc_rpg_mod` (`command/RpgCommands.java`) :
  `skills`, `deaths`, `xp`, `level`, `node`, `slot`, `power`, `hold`. Elles ont été écrites pour
  tester en jeu avant l'existence de `/competence`, et `level` comme `node` écrivent directement
  dans la capability, c'est-à-dire exactement ce que la règle ci-dessous interdit. Quand
  `/competence` est en place, **supprime `xp`, `level` et `skills`** (remplacés par `xp`,
  `niveau` et `info`), pour qu'il n'y ait qu'un seul chemin. Garde `deaths` tant que `info`
  n'affiche pas les compteurs de mort. Garde `node`, `slot`, `power` et `hold` jusqu'à ce que le
  handoff 3 livre l'écran et les touches : ce sont aujourd'hui les seuls moyens de choisir un nœud
  exclusif, de placer ou déclencher un pouvoir, et de simuler la touche de maintien.
- **Il n'existe aucun retrait d'attributs à la déconnexion.** Le seul mécanisme est
  `AttributeEffects.reapply(player)`, qui retire tous les modificateurs du mod puis remet ceux qui
  sont actifs. Il est appelé à la connexion, au respawn, au changement de dimension et à chaque
  montée de niveau (`SkillXp.grant`). « Le même chemin », plus bas, désigne donc **`reapply`** :
  un arbre désactivé doit simplement ne plus produire d'effets actifs, et `reapply` fait le reste.
  N'écris pas de second chemin de retrait.

## 1. Les commandes

Un seul arbre Brigadier sous `/competence`.

| Commande | Effet |
| --- | --- |
| `/competence xp <joueurs> <arbre> <montant>` | Ajoute de l'XP, ou en retire si négatif |
| `/competence niveau <joueurs> <arbre> <niveau>` | Fixe le niveau |
| `/competence mort <joueurs> <cause> <nombre>` | Fixe un compteur de cause de mort |
| `/competence reset <joueurs> [arbre]` | Efface — la seule commande qui efface |
| `/competence info [joueur]` | Affiche l'état |
| `/competence categorie <arbre> <etat>` | Actif, désactivé ou masqué |
| `/competence admin` | Ouvre l'écran d'administration |

Règles :

- **Tout passe par le chemin de progression normal.** Jamais une écriture
  directe dans la capability : sinon les montées de niveau, les déblocages de
  nœuds, l'application des effets et la synchronisation client ne partent pas.
  Une commande qui donne 10 000 XP doit produire exactement ce qu'aurait
  produit le jeu.
- Niveau de permission 2, sauf `info` sur soi-même, ouvert à tous.
- `<joueurs>` est un `EntityArgument.players()`, donc `@a` et les sélecteurs
  fonctionnent.
- Les complétions de `<arbre>` et de `<cause>` viennent du **registre chargé**,
  pas d'une liste en dur. Ajouter un barème doit ajouter sa complétion sans
  toucher au code. C'est le test qui dit si le chargeur est vraiment la source
  de vérité.
- Les messages de retour disent ce qui a changé et pour qui, au pluriel quand
  la cible est multiple.

## 2. L'état des catégories

Une catégorie est un des dix arbres. Trois états, pas deux interrupteurs.

- **Actif** — rien de particulier.
- **Désactivé** — l'arbre reste visible côté joueur, grisé et marqué comme
  coupé par l'administrateur. L'XP ne monte plus, les effets sont retirés.
- **Masqué** — absent de l'écran du joueur, comme s'il n'existait pas.

Dans les trois cas, **rien n'est effacé** : niveaux, XP, talents placés et
compteurs de mort sont conservés, et réactiver restaure les effets à
l'identique. Seul `reset` efface.

Points techniques :

- L'état vit dans une **`SavedData` du monde**, pas dans la configuration : il
  voyage avec la sauvegarde et se change en jeu. Une valeur de config ne sert
  qu'au premier démarrage d'un monde neuf.
- Désactiver retire les `AttributeModifier` de l'arbre **par le même chemin**
  que le reste, `AttributeEffects.reapply` (voir « Où en est le code »). Un
  second chemin, c'est l'assurance qu'ils se cumuleront à la réactivation.
- Le changement est synchronisé à tous les clients connectés : un arbre masqué
  doit disparaître sans reconnexion.
- `/competence admin` envoie un paquet d'ouverture **depuis le serveur**, après
  vérification du niveau de permission. Un client ne doit jamais pouvoir ouvrir
  cet écran de lui-même, ni en modifier l'état sans que le serveur revérifie.
- Prévois le compte de joueurs par arbre (combien en ont des niveaux, et le
  niveau le plus haut) : l'écran l'affiche avant qu'on coupe quelque chose.

## 3. Les barèmes envoyés au client

Les barèmes sont chargés comme données de serveur (`SkillTreeManager`, un
`SimpleJsonResourceReloadListener`). **Le client ne les reçoit jamais.** En solo,
ça passe par hasard : client et serveur intégré partagent le même `INSTANCE`
statique. Sur un serveur dédié, le client ne connaît ni les noms des arbres, ni
les paliers, ni les nœuds, ni la courbe d'XP, et tous les écrans du handoff 3
s'afficheraient vides.

- Un paquet serveur → client porte les arbres chargés, au moins ce que les
  écrans affichent : id, nom, couleur, `max_level`, `xp_curve`, paliers et nœuds
  (id, nom, description, `exclusive`, `trigger`, `requires_node`), et pour Mort
  les branches (cause, seuil) et les apparences (niveau, calque). Le plus simple
  et le plus sûr est d'envoyer le JSON source de chaque arbre et de le relire
  côté client avec le **même** `SkillTreeParser` : un seul format, pas de
  divergence possible.
- Envoyé à la connexion, et à tous les joueurs connectés après chaque `/reload`
  (`OnDatapackSyncEvent` couvre les deux cas).
- Côté client, un registre séparé (`ClientSkillTrees`, par exemple), distinct de
  `SkillTreeManager.INSTANCE`, qui reste la vérité du serveur. Sans cette
  séparation, le solo continuerait de masquer le bug.
- Le client s'en sert pour l'affichage, jamais pour décider : le serveur
  revérifie tout.
- Incrémente `PROTOCOL_VERSION`.

## Vérification

- Un test : la sérialisation des dix barèmes réels vers le paquet, puis leur
  relecture, redonne des arbres identiques.
- Des tests sur la commande : donner de l'XP fait franchir un palier et
  débloque le nœud attendu ; `reset` sur un seul arbre ne touche pas aux
  autres.
- Un test sur la bascule d'état : désactiver puis réactiver rend exactement les
  mêmes modificateurs d'attribut, sans doublon. C'est le bug le plus probable
  de ce lot.
- La complétion de `<arbre>` liste bien les dix identifiants lus dans les JSON.
