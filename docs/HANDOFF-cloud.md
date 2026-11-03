# Handoff 2 — session cloud

## Avant de lancer la session (pour toi, pas pour la session)

- **L'application GitHub de Claude doit être installée** sur `Askam/MinecraftRPGmod`
  (https://github.com/apps/claude), sinon la session ne peut ni cloner ni pousser.
- **Java 17** doit être présent dans l'environnement. Sinon, le plugin foojay déclaré dans
  `settings.gradle` essaiera d'en télécharger un, ce qui demande des domaines en plus.
- L'allowlist réseau et le script de démarrage se configurent **sur l'environnement cloud**, pas dans
  le dépôt : réglages Claude Code, section des environnements cloud. Un environnement en accès
  restreint ne résoudra pas les dépendances de Forge.

Domaines à autoriser :

```
maven.minecraftforge.net
libraries.minecraft.net
piston-meta.mojang.com
piston-data.mojang.com
resources.download.minecraft.net
repo1.maven.org
services.gradle.org
downloads.gradle.org
plugins.gradle.org
plugins-artifacts.gradle.org
api.foojay.io            ← seulement si Java 17 manque
```

Les quatre derniers ne figuraient pas dans la version précédente de ce handoff. Ce sont des cibles de
redirection probables (téléchargement de Gradle, artefacts du portail de plugins, JDK) et je ne les ai
pas vérifiés un par un. Si la résolution échoue, le message Gradle nomme l'hôte refusé : ajoute-le.

Deux choses à savoir :

- Un script de démarrage n'est mis en cache que s'il finit en cinq minutes environ. Le premier build
  Forge décompile Minecraft (environ 1 min 30 en local, sûrement plus en cloud), donc il risque de ne
  pas être mis en cache et chaque session le refera.
- **Lance d'abord une session courte de vérification**, dont le seul objectif est que
  `./gradlew build` passe. Si la résolution des dépendances échoue, une longue session ne fera
  qu'écrire du code invérifiable.

Depuis le dépôt : `claude --cloud "<la tâche>"`. Pour récupérer la session dans ton terminal ensuite :
`claude --teleport`.

---

# Tâche

## Où tu arrives

Un dépôt Forge 1.20.1 (47.4.0, Java 17, modid `mc_rpg_mod`, paquet `fr.needaname.mc_rpg_mod`) qui
compile. Il contient :

- dix barèmes de compétences dans `src/main/resources/data/mc_rpg_mod/competences/`, leur
  spécification dans `docs/SCHEMA.md`, et un contrôle de schéma `tools/valider.py` branché sur
  `check` (tâche `validerBaremes`) ;
- **un premier système de mort déjà écrit et testé en jeu** : capability persistante au respawn,
  détection des causes, synchro réseau. Voir `CLAUDE.md`, section « Code déjà présent ».

**Lis `CLAUDE.md` puis `docs/SCHEMA.md` en entier avant d'écrire une ligne.** `CLAUDE.md` contient
la règle « aucune valeur d'équilibrage dans le code », les pièges connus (dont deux bugs réels déjà
rencontrés) et les points ouverts que tu ne dois pas trancher seul.

## Ce que tu construis

Le moteur piloté par les données, et les effets qui n'ont pas besoin d'un client de jeu pour être
vérifiés.

1. **Le chargeur.** Un `SimpleJsonResourceReloadListener` branché sur `AddReloadListenerEvent`, qui
   lit `data/<namespace>/competences/*.json`. Un `type` d'effet inconnu ou un `schema` inattendu fait
   **refuser le fichier** avec un message nommant le fichier et le champ : jamais un comportement
   deviné, jamais un silence. Les règles de `tools/valider.py` sont le minimum que le chargeur Java
   doit aussi appliquer.

   Écris le passage JSON → modèle de façon testable **sans démarrer Minecraft** (Gson / `JsonObject`
   vers des records Java). Les `TagKey` et `ResourceKey` se résolvent dans une étape séparée. Les
   tests unitaires restent ainsi possibles sans `Bootstrap`.

2. **La capability du joueur** : niveau et XP par arbre, nœuds pris, compteurs de mort, emplacements
   de pouvoirs, recharges, sérialisés en NBT.

   Pars de l'existant plutôt que de tout recréer. `death/DeathStats` et `DeathCapabilityEvents`
   portent déjà le bon schéma de recopie dans `Clone` (`reviveCaps` / `invalidateCaps`, sans listener
   d'invalidation : lis le piège dans `CLAUDE.md` avant d'y toucher). Tu peux fusionner `DeathStats`
   dans une capability unique ou la garder à côté ; dans les deux cas, garde ce schéma de recopie. Il
   n'existe aucune sauvegarde de joueur réelle (seulement des mondes de dev), donc aucune migration NBT
   n'est nécessaire.

3. **La fusion des causes de mort.** La détection actuelle lit
   `data/mc_rpg_mod/mc_rpg_mod/death_categories/*.json`, un format provisoire écrit avant les barèmes.
   La source de vérité est désormais `counters.per_cause` de `mort.json` (`damage_type`,
   `damage_type_tag`, `source_entity`, `excluding_source_entity`). Fais lire ces counters par le
   chargeur, branche `DeathEventHandler` dessus, puis supprime `death_categories/`, `DeathCategory` et
   `DeathCategoryManager`. Garde `/mc_rpg_mod deaths` en état de marche : c'est l'outil de test en jeu
   du propriétaire.

   Une mort compte dans **toutes** les causes qu'elle satisfait, ou dans la première seulement ?
   `SCHEMA.md` ne le dit pas, mais `excluding_source_entity` sur `explosion` suggère que les causes
   sont pensées comme disjointes. Implémente-les comme disjointes et signale le choix dans la PR.

   Attention : le compteur `creeper` de `mort.json` utilise `damage_type: minecraft:explosion`, alors
   qu'une explosion de creeper a le type `minecraft:player_explosion`. Tel quel, il ne s'incrémente
   jamais. **Ne corrige pas le barème** (c'est une décision du propriétaire) : ajoute un test qui
   documente le problème et signale-le dans la PR.

4. **Le répartiteur d'effets.** Une interface par type du catalogue, et une table qui associe le
   `type` lu au code qui l'applique. C'est la pièce centrale : tout le reste s'y branche.

5. **Les sources d'XP**, dans leurs deux formes : statistique vanilla cumulative, et événement
   ponctuel. Plus `xp_on_death` pour Mort, dont le seuil se compte en temps de jeu **actif**. Définis
   « actif » de façon explicite (par exemple : le joueur a bougé ou agi dans les N dernières secondes)
   et signale cette définition dans la PR, car le barème ne la précise pas.

6. **Les effets serveur.** Dix-neuf actions et trente-six drapeaux au total ; un petit script dans
   `tools/` peut les lister depuis les JSON. Commence par ceux qui se testent sans client :
   `attribute`, `damage_taken`, `damage_dealt`, `fall_distance`, `drop_bonus`, `breeding`, `air`.

   **Décision ouverte, ne la tranche pas :** 15 des 36 `flag` sont des multiplicateurs sur une
   grandeur vanilla nommée. Un type `tweak` les replierait, mais ce n'est pas décidé. Implémente-les
   comme des `flag` et structure le code pour qu'un repli ultérieur reste mécanique (par exemple un
   seul gestionnaire générique de multiplicateur derrière ces quinze noms).

7. **`break_shape`**, qui porte Veinage, Abattage, Moisson et Carrier. Quatre formes : `vein`, `tree`,
   `area`, `column` (`column` est utilisé par `minage.json` mais absent de `SCHEMA.md` ; complète la
   doc et `tools/valider.py` si tu le formalises). Respecte `max_blocks`, `filter`,
   `stamina_per_block`, et le fait que ces nœuds ne s'activent que sur maintien d'une touche : l'état
   de la touche arrive par paquet, il ne se devine pas.

8. **Le réseau.** Le `SimpleChannel` existe déjà (`network/RpgNetwork`, avec un paquet de synchro des
   compteurs de mort). Ajoute-y la synchro du reste de la capability (ce dont l'interface aura besoin :
   niveaux, XP, nœuds, recharges), l'état de la touche de maintien vers le serveur, et les
   déclenchements de pouvoirs. Incrémente `PROTOCOL_VERSION` dès qu'un format de paquet change.

9. **Les recharges** : `nodeId` vers le `gameTime` du dernier usage, comparé à `level.getGameTime()`.
   Un horodatage, pas un décompte : pas de boucle de tick, et une recharge survit à une déconnexion.

## Ce que tu ne construis pas

Rien de visuel. Cette session n'a pas de client de jeu et ne peut rien vérifier à l'écran :

- l'écran de compétences et l'écran des emplacements de pouvoirs (une maquette d'interface est
  préparée à part)
- les `RenderLayer` : aura rouge, peau de cendre, armure spectrale
- la rangée d'icônes de recharge au-dessus de la barre d'objets
- les `KeyMapping` et la détection du double appui côté client

Laisse des points d'ancrage propres côté serveur pour que ces pièces s'y branchent plus tard, et
n'écris pas de code client que tu ne peux pas regarder tourner. Seule exception : la réception des
paquets (comme l'existant `network/ClientPacketHandler`), isolée derrière `DistExecutor`.

Tu n'enregistres pas non plus l'item `mc_rpg_mod:resine` (référencé par `bucheronnage.json`) : il
lui faut une texture et un modèle. Le `drop_bonus` qui le donne doit gérer un item absent du registre
en le journalisant, sans planter.

## Vérification

- `./gradlew build` passe, et `check` exécute le validateur.
- **JUnit n'est pas encore configuré** : ajoute `testImplementation` JUnit 5 et
  `test { useJUnitPlatform() }` dans `build.gradle`, puis vérifie que `./gradlew test` tourne vraiment
  (un test volontairement faux doit faire échouer le build).
- Tests unitaires sur le chargeur : un fichier valide se charge, un `type` inconnu est refusé, un
  palier à deux nœuds sans `exclusive` est refusé. Les dix barèmes réels se chargent sans erreur.
- Tests sur la courbe d'XP et sur la table de reproduction (`faune.json` : 1 % dix petits, 19 % deux,
  80 % un seul). Pour la courbe, teste la **formule** `round(base × n^exponent)`, qui donne 848 / 2 998
  / 5 395 aux niveaux 10 / 30 / 50 avec 60 et 1,15. Les exemples de `SCHEMA.md` (846 / 2 940 / 5 220)
  ne correspondent pas à cette formule ; ne les « corrige » pas en changeant la formule, signale
  l'écart dans la PR.

**Tu ne peux pas lancer Minecraft.** Ne prétends pas avoir testé en jeu. Dis explicitement, dans la
description de chaque pull request, ce qui reste à vérifier sur une machine avec un client. Rappelle
en particulier que la fusion des causes de mort (point 3) touche du code déjà validé en jeu, et que le
propriétaire doit refaire le test « mourir, respawn, `/mc_rpg_mod deaths` ».

## Pièges

Ils sont dans `CLAUDE.md`, section « Pièges connus ». Les deux plus coûteux :

- **La capability est recréée au respawn.** Sans recopie correcte dans `Clone`, le compteur de Mort
  est perdu à l'instant précis où il s'incrémente. Ce bug s'est déjà produit une fois, sous une forme
  subtile (listener d'invalidation).
- **`faune.json` exclut les villageois de la table de reproduction.** Ne retire pas cette exclusion.

## Découpage

Si tu dois t'arrêter, arrête-toi sur une frontière nette et dis où tu en es. L'ordre ci-dessus est
fait pour ça : chaque point se tient seul et se construit sur le précédent. Une pull request par bloc
cohérent vaut mieux qu'une seule énorme. Travaille sur des branches, jamais directement sur `main`.
