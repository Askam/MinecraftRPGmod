# MC RPG Mod (`mc_rpg_mod`)

Mod RPG maison pour un serveur Minecraft privé entre amis (une dizaine de joueurs, niveaux mixtes).
Il remplace Project MMO avec une règle : **des bonus, jamais de verrous** — aucun objet, bloc ou arme
n'est interdit faute de niveau.

Dix arbres de compétences de 50 niveaux, entièrement pilotés par des JSON. Neuf progressent par
l'activité (XP). Le dixième, **Mort**, progresse à chaque mort du joueur, tient un compteur par cause
de mort et débloque des branches selon ces compteurs.

Le pack du serveur compte ~62 mods (Create, Botania, Aether, Cataclysm, YUNG's, etc.) : toujours
viser les blocs, entités et dégâts par **tags** (`#minecraft:logs`, `#forge:bosses`) plutôt que par
liste d'ids, pour couvrir le contenu moddé.

## Règle absolue : aucune valeur d'équilibrage dans le code

Tout nombre d'équilibrage (XP, seuils, multiplicateurs, recharges, rayons, chances) vit dans les
barèmes JSON. Le code lit, il ne décide pas. C'est ce qui permet de retoucher un barème et de le
recharger avec `/reload` sans recompiler. Un champ manquant dont le code aurait besoin s'ajoute au
schéma (`docs/SCHEMA.md` + `tools/valider.py`), jamais en constante Java.

## Barèmes

- Les dix arbres : `src/main/resources/data/mc_rpg_mod/competences/*.json`
- Spécification complète du format : `docs/SCHEMA.md` — **à lire en entier avant de toucher au
  chargeur ou aux effets**. Catalogue fermé de 12 types d'effets : un `type` inconnu ou un `schema`
  inattendu fait refuser le fichier, avec un message nommant fichier et champ.
- Validation : `python3 tools/valider.py src/main/resources/data/mc_rpg_mod/competences/*.json`
  (sous Windows : `python -X utf8 …`). Elle tourne aussi dans `./gradlew check`, donc dans
  `./gradlew build` : un barème invalide fait échouer le build.
- Tag de dégâts propre au mod : `data/mc_rpg_mod/tags/damage_type/is_combat.json` (attaques directes
  et projectiles ; ni chute, ni feu, ni noyade, ni explosion). Utilisé par `combat.json`.

## Build

- Minecraft **1.20.1**, Forge **47.4.0** (aligné sur le serveur), mappings `official`, **Java 17**.
- `./gradlew build` — compile, valide les barèmes, produit `build/libs/mc_rpg_mod-<version>.jar`.
- Le premier build décompile Minecraft et prend plusieurs minutes ; c'est normal.
- La tâche de validation cherche `python3` (ou `python` sous Windows) ; surcharge : `-Ppython=<cmd>`.
- API dépréciées en 47.4 à éviter : `new ResourceLocation(ns, path)` → `ResourceLocation.fromNamespaceAndPath`,
  `FMLJavaModLoadingContext.get()` → contexte injecté dans le constructeur `@Mod`.

## Code déjà présent (testé en jeu)

Un premier système de mort existe et a été vérifié en jeu (solo, client de dev) :

- `death/DeathStats` + `DeathStatsProvider` : capability, total + compteur par catégorie, NBT.
- `death/DeathCapabilityEvents` : attache + recopie dans `PlayerEvent.Clone` (vérifié : les compteurs
  survivent au respawn).
- `death/DeathEventHandler` : `LivingDeathEvent` en `LOWEST`, une mort annulée ne compte pas.
- `death/DeathCategory` + `DeathCategoryManager` : catégories de mort en JSON dans
  `data/mc_rpg_mod/mc_rpg_mod/death_categories/`, première qui matche par `priority`, sinon `other`.
- `network/` : `SimpleChannel`, paquet de synchro des compteurs vers soi et les joueurs qui suivent
  (connexion, respawn, dimension, mort, début de suivi).
- Commandes de debug : `/mc_rpg_mod deaths [joueur]` (serveur), `/mc_rpg_modclient deaths` (ce que
  le client a reçu).

**À fusionner** : ces `death_categories` font doublon avec `counters.per_cause` de `mort.json`, qui
est la source de vérité du design. Le chargeur des compétences doit reprendre la détection des causes
depuis `mort.json` ; les `death_categories` disparaîtront alors (la capability, le Clone et la synchro
restent valables).

## Pièges connus

- **La capability est recréée au respawn** : la recopier dans `PlayerEvent.Clone`, avec
  `original.reviveCaps()` avant et `original.invalidateCaps()` après. Ne **pas** enregistrer
  `event.addListener(provider::invalidate)` à l'attache : l'invalidation du `LazyOptional` est
  définitive et `reviveCaps()` ne la défait pas — les compteurs étaient perdus à chaque mort
  (bug réel, corrigé).
- **`AttributeModifier`** : UUID fixe, retirer avant de réappliquer, sinon cumul à chaque reconnexion.
- **Types de dégâts data-driven** en 1.20.1 : comparer par id ou tag (`source.is(...)`), jamais par
  instance.
- **Une explosion de creeper est `minecraft:player_explosion`**, pas `minecraft:explosion` :
  `DamageSources.explosion(cause, attacker)` passe en `player_explosion` dès que les deux entités sont
  non nulles, et le creeper est sa propre source indirecte. Filtrer les explosions par le tag
  `minecraft:is_explosion`.
- **Recharges** : `nodeId → gameTime` du dernier usage, comparé à `level.getGameTime()`. Pas de
  décompte en tick ; une recharge survit à une déconnexion.
- **`break_shape` et pouvoirs côté serveur uniquement**, après vérification de la possession du nœud
  et de la recharge. Jamais l'effet côté client.
- **`faune.json` exclut les villageois de la table de reproduction** : ne pas retirer (dix bébés
  villageois d'un coup font sauter le watchdog).
- Les deux déclencheurs `client_*` (saut en plein vol, double appui) sont les seuls que le serveur
  ne voit pas : paquet client, validé côté serveur.
- Code client (rendu, écrans, touches) : uniquement dans des classes chargées côté client
  (`@Mod.EventBusSubscriber(value = Dist.CLIENT)`, `DistExecutor`), jamais référencées depuis du code
  commun — sinon crash au démarrage du serveur dédié.

## Points ouverts connus

- `mort.json`, compteur `creeper` : déclaré avec `damage_type: minecraft:explosion`, il ne matchera
  jamais (voir piège ci-dessus). À corriger dans le barème, décision du propriétaire.
- `bucheronnage.json` référence l'item `mc_rpg_mod:resine`, qui n'est pas encore enregistré.
- `break_shape` : `minage.json` utilise `shape: "column"` (Carrier, `max_blocks: 3`), mais
  `docs/SCHEMA.md` ne liste que `vein`, `tree`, `area`. Les quatre formes sont à implémenter ; la doc
  est à compléter.
- 15 des 36 effets `flag` sont des multiplicateurs sur une grandeur vanilla nommée (`dig_speed`,
  `durability`, …). Un type `tweak { quantity, multiplier }` les replierait en un seul chemin de code.
  **Décision non prise** par le propriétaire : ne pas trancher seul.

## Ce qui ne se teste pas sans client de jeu

Rendu (aura, couches d'apparence, `RenderLayer`), écrans (arbre de compétences, emplacements de
pouvoirs), icônes de recharge, `KeyMapping` et double appui, sensation du double saut / dash,
synchro vers un *autre* joueur (deux clients nécessaires). Sans client, ne jamais prétendre l'avoir
vérifié : le lister explicitement comme restant à tester.
