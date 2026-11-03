# Handoff 3b — reprise locale du visuel

**Pour une session Claude Code (ou un dev) LOCALE, jeu lançable.** Le handoff 3
a été écrit depuis une session cloud qui ne peut rien afficher ni tester : tout
le code client ci-dessous **compile** (`./gradlew build` vert, 60 tests), mais
**aucune interface, aucun calque de rendu, aucune touche n'a été vu tourner**.
C'était un choix assumé du propriétaire ; ce handoff dit quoi vérifier et finir.

> Règle inchangée : le client affiche et envoie des intentions, le serveur décide.
> Rien d'équilibrage dans le code client (tout vient des données synchronisées).

## Démarrer

1. **Exporte les 14 maquettes en PNG dans `docs/maquettes/`** (voir
   `HANDOFF-3-visuel.md`) : elles fixent l'architecture de l'information et les
   états, pas les pixels. Le code actuel est une disposition fonctionnelle
   (listes + boutons), à retravailler à l'œil.
2. **Teste au moins une fois en serveur dédié**, pas seulement en solo :
   `./gradlew runServer` puis `./gradlew runClient` connecté en `localhost`. En
   solo, client et serveur partagent l'`INSTANCE` statique et masquent les bugs
   de synchro ; les écrans lisent `ClientSkillTrees` (rempli uniquement par le
   paquet serveur), jamais `SkillTreeManager.INSTANCE`.
3. Commandes utiles pour se mettre en situation (toujours présentes) :
   `/competence xp <joueurs> <arbre> <montant>`, `/competence niveau …`,
   `/competence mort …`, `/competence categorie <arbre> <etat>`,
   `/competence admin`. Et l'outillage bas niveau `/mc_rpg_mod node|slot|power|hold`.

## Ce qui est déjà câblé (à vérifier à l'œil)

- **Touches** (`RpgKeyMappings`, remappables) : `K` ouvre la vue d'ensemble
  (ajout — le handoff 3 ne fixait pas de touche d'ouverture), `C` maintien,
  `R`/`G`/`X` pouvoirs. Détection double saut et double appui avant dans
  `ClientInputHandler`.
- **Écrans** (`client/screen`) : vue d'ensemble, arbre, pouvoirs, admin. Les cinq
  états de nœud sont marqués par **symbole + libellé + couleur** (jamais la
  couleur seule) — à confirmer lisibles sans distinction du rouge/vert.
- **Rendu** (`client/render`) : aura rouge, peau de cendre, armure spectrale,
  pilotés par `ClientAppearances` (drapeaux calculés serveur et envoyés aux
  observateurs via `SyncAppearancePacket`).
- **HUD** (`client/hud/CooldownHud`) : rangée de recharges au-dessus de la barre
  d'objets, visible seulement pendant une recharge, effacée ~2 s après.

## À vérifier en jeu (la raison d'être de la session locale)

- Les cinq états de nœud distinguables **sans la couleur seule**.
- Les écrans tiennent à **toutes les échelles d'interface** (pas seulement la
  tienne), et à largeur de fenêtre variable.
- **Aura et peau de cendre visibles sur un AUTRE joueur** (deux clients, ou
  runServer + deux runClient), pas seulement sur soi.
- Le **HUD ne laisse rien** à l'écran une fois les recharges finies.
- **Masquer une catégorie** depuis l'écran d'admin la fait disparaître chez un
  joueur connecté, **sans reconnexion** ; désactiver la grise et retire ses
  effets, réactiver les rend à l'identique (le bug le plus probable : doublons de
  modificateurs — c'est déjà fait par le chemin unique `AttributeEffects.reapply`,
  à confirmer).
- **Shaders actifs** : vérifie les calques avec Oculus + Complementary, pas
  seulement en vanilla. Une joueuse est sur **Mac** (shaders déjà problématiques).
- Le glint d'armure spectrale rendu **seul** (sans texture dessous) donne bien
  l'effet voulu ; ne pas le remplacer par une texture transparente.

## Ce qui reste à finir (points d'ancrage laissés dans le code)

1. **Exécution des pouvoirs/reflexes.** `PowerActivation` et `ReflexActivation`
   valident la possession et la recharge, posent la recharge, puis **journalisent**
   sans exécuter l'effet (impulse, dash, summon_ally, whirlwind, grow_crops,
   revive_in_place…). C'est l'étape suivante : brancher l'exécution des `ability`.
2. **Armure spectrale.** Le drapeau `spectral` est toujours faux : l'état
   transitoire `spectral_armour_active` n'est pas suivi côté serveur. Le relier à
   l'ability de respawn, et détecter la fin sur le **vidage du cadavre (Corpse)**.
3. **Roulade.** Gérée côté serveur par `isShiftKeyDown` (`FallDistanceEffects`).
   Le handoff veut un **appui frais d'accroupissement dans les 5 ticks avant
   l'impact** : détection client + paquet, à ajouter dans `ClientInputHandler`.
4. **Double saut / double appui.** Détections heuristiques (seuils dans
   `ClientInputHandler`) à régler au ressenti ; les 3 autres directions du double
   appui ne sont pas câblées.
5. **Écran des pouvoirs.** L'auto-placement des trois premiers pouvoirs débloqués
   n'est pas fait (placement manuel seulement).
6. **Rafraîchissement des écrans.** Après une intention envoyée, l'écran se
   réouvre pour relire l'état après synchro (léger décalage) — à remplacer par un
   vrai rafraîchissement sur réception de `SyncSkillsPacket`/`SyncCategoriesPacket`.
7. **Mise en page.** Transposer les maquettes (panneau de détail de l'arbre, deux
   cases côte à côte d'un palier exclusif, barres d'XP, pastilles) ; le code actuel
   est volontairement minimal.

## Quand retirer l'outillage de test

Une fois l'écran d'arbre et les touches **confirmés en jeu**, retire
`/mc_rpg_mod node`, `slot`, `power` et `hold` (`command/RpgCommands.java`) : ils ne
servaient qu'à remplacer l'écran et les touches en attendant. **Ne les retire pas
tant que le visuel n'est pas validé** — c'est aujourd'hui le seul chemin sûr.

## État réseau

`PROTOCOL_VERSION = 7`. Paquets ajoutés par le handoff 3 : `ClientReflexPacket`,
`ChooseNodePacket`, `AssignPowerPacket`, `SetCategoryPacket` (client→serveur,
revérifiés), `SyncAppearancePacket` (serveur→client). Pense à réincrémenter à tout
changement de format.
