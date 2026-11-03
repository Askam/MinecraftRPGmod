# Handoff 3 — la partie visuelle

**Session locale obligatoire.** Tout ce qui suit se juge à l'œil et se teste en
lançant le jeu. Une session cloud ne peut rien en vérifier.

**Prérequis : le handoff 2b doit être livré**, en particulier son point 3 (les
barèmes envoyés au client). Les écrans lisent les arbres depuis le registre
client (`ClientSkillTrees` ou équivalent), **jamais depuis
`SkillTreeManager.INSTANCE`** : en solo les deux semblent marcher, mais sur le
serveur dédié seul le registre client est rempli. Teste au moins une fois
contre un serveur dédié (`./gradlew runServer` + `runClient` connecté en
`localhost`), pas seulement en solo.

Quand l'écran d'arbre et les touches sont en place, supprime les commandes de
test `/mc_rpg_mod node`, `slot`, `power` et `hold` (`command/RpgCommands.java`) :
elles ne servaient qu'à remplacer l'écran et les touches en attendant.

## Avant de commencer : les maquettes

Quatorze maquettes existent, faites dans Claude Design. **Exporte-les en PNG et
pose-les dans `docs/maquettes/`** avant de lancer la session : Claude Code lit
les images, et une maquette vaut mieux qu'une description.

- `Main` — la vue d'ensemble, grille de dix compétences
- Dix écrans d'arbre, un par compétence
- `Pouvoirs` — les trois emplacements et la liste des pouvoirs débloqués
- `Admin` — les trois états par catégorie, réservé aux opérateurs

Les maquettes fixent **l'architecture de l'information et les états**, pas les
pixels. Elles sont dessinées en HTML à 960 × 640 : ne les transpose pas
littéralement. Suis les conventions d'interface de Minecraft pour les widgets,
les marges et les tailles de police, et garde de ces maquettes la hiérarchie,
le vocabulaire et surtout les **états visuels**, qui sont la partie coûteuse à
réinventer.

Les cinq états d'un nœud, dans l'ordre de lecture :

| État | Lecture |
| --- | --- |
| Acquis | vert, coché |
| À placer | doré, mis en avant |
| Non choisi | gris, verrouillé — l'autre option du palier a été prise |
| Verrouillé | gris, cadenas — le niveau n'est pas atteint |
| Désactivé | grisé avec une mention explicite, quand l'administrateur a coupé l'arbre |

## Ce que tu construis

### Les écrans

1. **Vue d'ensemble** — les dix compétences, barre d'XP, niveau, pastille dorée
   quand un talent attend d'être placé. Une compétence masquée par
   l'administrateur n'apparaît pas ; une compétence désactivée apparaît grisée
   avec la mention.
2. **Écran d'arbre** — les cinq paliers, les choix exclusifs à deux cases, le
   panneau de détail à droite. Un palier à deux cases n'en laisse prendre
   qu'une.
3. **Écran des pouvoirs** — trois emplacements que le joueur remplit depuis la
   liste de ses pouvoirs débloqués. Les trois premiers débloqués s'y placent
   tout seuls ; tout reste modifiable.
4. **Écran d'administration** — ouvert par `/competence admin`, donc par un
   paquet venu du serveur. Le client ne l'ouvre jamais seul, et chaque
   changement est revérifié côté serveur.

### Le rendu sur le joueur

Des `RenderLayer` ajoutés via `EntityRenderersEvent.AddLayers`, tous pilotés
par les compteurs de l'arbre de Mort, donc dépendants de la synchronisation
réseau — le client doit connaître les compteurs pour décider d'afficher quoi
que ce soit.

- **Aura rouge** — apparaît au niveau 40 de Mort, s'intensifie au 50
- **Peau de cendre** — assombrit durablement la peau, débloquée par la branche
  « Né des flammes »
- **Armure spectrale** — se rend avec le `RenderType` de glint d'armure **seul**,
  sans passe de texture en dessous. Un second calque, après celui de l'aura. La
  fin de l'effet se détecte sur le vidage du cadavre de Corpse.

### Le HUD

**Rien en permanence à l'écran** — c'est une exigence, pas une préférence. Une
rangée de trois icônes apparaît au-dessus de la barre d'objets uniquement
pendant une recharge, et s'efface deux secondes après la dernière.

### Les entrées

- **Touche de maintien** (`C` par défaut) — tenue pendant qu'on casse, pour
  Veinage, Abattage, Moisson et Carrier. L'état part au serveur **à chaque
  changement**, pas à chaque bloc.
- **Trois touches d'emplacement** (`R`, `G`, `X`) — déclenchent le pouvoir
  placé. Paquet au serveur, qui vérifie possession et recharge avant d'agir.
- **Double saut** — le serveur ne voit pas un appui sur saut en plein vol.
  Détection client, paquet, validation serveur.
- **Double appui de direction** pour Élan. Vers l'avant, **il faut déjà
  sprinter** : le double appui sur avancer est le geste de sprint de vanilla,
  et le dash ne doit pas se consommer à chaque départ en course. Les trois
  autres directions n'ont aucun sens vanilla, donc elles sont libres.
- **Roulade** — l'accroupissement doit être un **appui frais**, détecté dans
  les cinq ticks avant l'impact, et non la touche tenue pendant toute la
  descente. Sinon ce n'est plus un geste, c'est une taxe sur la touche Maj.

Toutes les touches sont remappables. `C`, `R`, `G` et `X` sont libres en
vanilla, et `V` est volontairement évité : c'est la touche de parole de Simple
Voice Chat, qui est dans le modpack.

## Ce que tu ne fais pas

- Tu ne touches pas aux barèmes JSON ni au chargeur
- Tu n'écris aucune valeur d'équilibrage dans le code client : tout vient des
  données synchronisées
- Tu ne déplaces aucune décision de jeu côté client. Le client affiche et
  envoie des intentions ; le serveur décide.

## Vérification

Lance le jeu. C'est la raison d'être de cette session.

- Les cinq états de nœud sont distinguables **sans couleur seule** — un joueur
  qui distingue mal le rouge du vert doit s'y retrouver
- Les écrans tiennent à toutes les échelles d'interface de Minecraft, pas
  seulement à celle de ta machine
- L'aura et la peau de cendre se voient sur un autre joueur, pas seulement sur
  soi
- Le HUD ne laisse rien à l'écran une fois les recharges finies
- Masquer une catégorie depuis l'écran d'administration la fait disparaître
  chez un joueur connecté, sans reconnexion
- Les shaders sont actifs sur ce serveur : vérifie les calques de rendu avec
  Oculus et Complementary allumés, pas seulement en rendu vanilla. Une joueuse
  est sur Mac, où les shaders ont déjà posé problème.

## Pièges

- Les `RenderLayer` tournent sur le modèle du joueur : attention aux deux
  modèles (bras fins et bras larges) et à la couche externe de la peau.
- Un écran plein ne doit pas bloquer le jeu côté serveur : rien de lourd dans
  le rendu, pas de requête par image.
- Le glint d'armure est un `RenderType` particulier ; rendu seul, sans texture
  dessous, c'est ce qui donne l'effet spectral. Ne le remplace pas par une
  texture transparente, le résultat n'est pas le même.
