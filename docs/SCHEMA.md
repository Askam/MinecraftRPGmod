# Barèmes — schéma des fichiers de compétences

Un fichier par arbre, dans
`src/main/resources/data/<modid>/competences/<id>.json`, chargé par un
`SimpleJsonResourceReloadListener` branché sur `AddReloadListenerEvent`.
Les dix arbres partagent ce schéma, Mort compris : il ajoute des clés, il n'en
change aucune.

Tout ce qui est un nombre est ici. Rien dans le code ne doit contenir une
valeur d'équilibrage — c'est la condition pour retoucher un barème sans
recompiler, et pour que le serveur recharge d'un `/reload`.

## Racine

| Clé | Type | Rôle |
|---|---|---|
| `schema` | entier | Version du format. Un chargeur qui lit un numéro inconnu refuse le fichier au lieu de deviner. |
| `id` | chaîne | Identifiant interne, celui des commandes et de la sauvegarde. |
| `name` | chaîne | Nom affiché. |
| `color` | hex | Couleur de la barre dans l'interface. |
| `max_level` | entier | 50 pour les dix arbres. |
| `xp_curve` | objet | `{ base, exponent }` → XP pour passer au niveau *n* = `round(base × n^exponent)`. Avec 60 et 1,15 : 846 au niveau 10, 2 940 au niveau 30, 5 220 au niveau 50. |
| `xp_sources` | tableau | Ce qui fait monter l'arbre. Deux formes : `{ stat, per, xp }` pour une statistique vanilla cumulative (le pas, et l'XP par pas), ou `{ on, filter?, require?, xp }` pour un événement ponctuel — `break_block`, `harvest_mature`, `plant`, `breed`, `tame`, `shear`, `trade`, `kill`, `ranged_hit`, `shield_block`, `eat`, `damage_survived`, `use_item`, `surface_from_depth`. |
| `tiers` | tableau | Les paliers. |

Mort remplace `xp_sources` par `xp_on_death` et ajoute `counters`,
`branches` et `appearances`.

## Paliers et nœuds

```json
{ "level": 10, "exclusive": true, "nodes": [ … ] }
```

`exclusive` à `true` rend les nœuds du palier mutuellement exclusifs : le
joueur en prend un. Absent ou `false`, le palier n'a qu'un nœud et il est
offert.

Un nœud porte `id`, `name`, `description`, un `trigger` facultatif, un
tableau `effects`, et éventuellement `requires_node` quand il améliore un
nœud antérieur (Ossuaire sur Appel d'outre-tombe).

## Déclenchement

| `family` | Entrée | Exemples |
|---|---|---|
| absent | aucune, l'effet est permanent | Pas léger, Armure de l'oubli |
| `reflex` | une condition, ou un geste déjà dans les mains du joueur | Roulade, Riposte, Double saut, Résurrection |
| `hold` | la touche de récolte maintenue, `C` | Veinage, Abattage, Moisson |
| `power` | un emplacement, `R` / `G` / `X` | Prospection, Floraison, Appel, Tourbillon, Appel d'outre-tombe |

Un `reflex` précise `on` (l'événement) et parfois `require` (les conditions
supplémentaires). Les valeurs de `on` qui commencent par `client_` sont les
deux seules que le serveur ne peut pas observer — un appui sur saut en plein
vol, un double appui — et imposent un paquet client validé côté serveur.

## Catalogue des effets

Jeu fermé : un `type` inconnu fait refuser le fichier. Chaque type
correspond à un seul chemin de code.

| `type` | Clés | Ce que ça fait |
|---|---|---|
| `attribute` | `attribute`, `operation`, `value` | Un modificateur d'attribut vanilla. À retirer avant réapplication, sinon il se cumule à chaque reconnexion. |
| `movement` | `aspect`, `multiplier` | `climb`, `swim`, `sprint_cost`. |
| `damage_taken` | `multiplier`, `when` | Dégâts reçus, filtrés par type ou tag de dégâts. |
| `damage_dealt` | `multiplier`, `when` | Dégâts infligés, filtrés par entité, tag, ou vie restante de la cible. |
| `fall_distance` | `subtract_blocks` | Retire des blocs au compteur avant le calcul vanilla, qui en retire déjà 3. |
| `drop_bonus` | `filter`, `chance`, `extra`, `item`? | Butin supplémentaire sur un tag de blocs. `item` fait tomber autre chose que le bloc lui-même — la résine de Sève. |
| `break_shape` | `shape`, `max_blocks`, `filter`, `stamina_per_block` | `vein`, `tree`, `area`. Le cœur de Veinage, Abattage et Moisson. |
| `breeding` | `table` | Table de poids : `[{ weight, children }]`. |
| `air` | `reserve_multiplier`, `regen_ticks_per_bubble`, `drown_delay_ticks`, `drown_multiplier` | Tout ce qui touche à l'apnée. |
| `ability` | `action`, `cooldown_ticks`, + paramètres | Une action nommée. `cooldown_ticks` à 0 = pas de recharge. Avec `overrides: "<id de nœud>"`, l'effet ne fait que corriger des champs de celui-là et hérite du reste — c'est ainsi qu'Ossuaire améliore le squelette sans redire la recharge. |
| `appearance` | `layer`, `permanent` ou `while` | Un `RenderLayer`. N'a aucun effet de jeu. |
| `flag` | `flag`, + paramètres | Un interrupteur que le code lit. L'échappatoire pour les comportements uniques, afin de ne pas inventer un type par nœud. |

## Mort

Trois ajouts.

`xp_on_death` — `{ flat, requires_active_seconds_since_last_death }`. Tarif
fixe, et un seuil de 5 minutes de jeu **actif**. Pas de modulation par le
temps de survie : elle ferait converger bon et mauvais joueur, ce qui est
l'inverse du but de l'arbre.

`counters` — un compteur total, et un par cause. Chaque cause se décrit par
un `damage_type`, un `damage_type_tag`, et au besoin une `source_entity`
pour séparer le creeper de l'explosion générique. En 1.20.1 les types de
dégâts sont data-driven : les comparer par identifiant ou par tag, jamais
par instance.

`branches` — des nœuds ouverts par un compteur de cause, pas par le niveau.
Chaque entrée porte `cause` et `threshold`. C'est ce qui fait qu'un joueur
qui tombe souvent progresse différemment d'un joueur qui brûle.

`appearances` — des couches de rendu attachées à un niveau, invisibles dans
l'arbre. Le client doit connaître les compteurs pour décider de les
afficher, donc elles imposent une synchronisation réseau.

## Pièges

- La capability est recréée au respawn : la recopier dans `PlayerEvent.Clone`
  en testant `isWasDeath()`, sinon le compteur de Mort est perdu à l'instant
  où il s'incrémente.
- Les recharges se stockent en `nodeId → gameTime du dernier usage`, comparé
  à `level.getGameTime()`. Un horodatage plutôt qu'un décompte : pas de
  boucle de tick, et une recharge survit à une déconnexion.
- `break_shape` et les pouvoirs s'exécutent côté serveur après vérification
  de la possession du nœud et de la recharge. Jamais l'effet côté client.
- Les niveaux et XP des maquettes sont des valeurs d'illustration, pas la
  courbe : celle-ci est dans `xp_curve`.
