#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Contrôle de schéma des barèmes. Sort 1 si un fichier est invalide.

    python3 valider.py            # tous les .json du dossier du script
    python3 valider.py a.json b.json

À brancher en tâche Gradle avant le build : un barème cassé doit faire
échouer la compilation, pas démarrer un serveur qui refuse un arbre.
"""
import json
import pathlib
import sys

EFFETS = {
    "attribute", "movement", "damage_taken", "damage_dealt", "fall_distance",
    "drop_bonus", "break_shape", "breeding", "air", "ability", "appearance", "flag",
}
FAMILLES = {"reflex", "hold", "power"}
OPERATIONS = {"addition", "multiply_base", "multiply_total"}
RACINE = {"schema", "id", "name", "color", "max_level", "xp_curve", "tiers"}
CLES_MORT = {"xp_on_death", "counters", "branches", "appearances"}


def valider(chemin):
    erreurs = []
    err = erreurs.append
    try:
        d = json.loads(chemin.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        return [f"JSON illisible : {e}"]

    for cle in sorted(RACINE - set(d)):
        err(f"clé de racine manquante : {cle}")
    if d.get("schema") != 1:
        err(f"schema attendu 1, lu {d.get('schema')!r}")
    if d.get("id") != chemin.stem:
        err(f"id {d.get('id')!r} ne correspond pas au nom de fichier {chemin.stem!r}")

    a_xp = "xp_sources" in d or "xp_on_death" in d
    if not a_xp:
        err("ni xp_sources ni xp_on_death")

    ids = {}
    niveaux = []

    def verifier_effets(effets, ou):
        for e in effets:
            t = e.get("type")
            if t not in EFFETS:
                err(f"{ou} : type d'effet inconnu {t!r}")
            if t == "attribute" and e.get("operation") not in OPERATIONS:
                err(f"{ou} : operation invalide {e.get('operation')!r}")
            # Un effet qui en corrige un autre hérite des champs qu'il ne redit pas.
            if t == "ability" and "cooldown_ticks" not in e and "overrides" not in e:
                err(f"{ou} : ability sans cooldown_ticks (mettre 0 si aucune)")

    def verifier_noeud(n, ou):
        for cle in ("id", "name", "description", "effects"):
            if cle not in n:
                err(f"{ou} : nœud sans {cle}")
        nid = n.get("id")
        if nid in ids:
            err(f"{ou} : id de nœud en double {nid!r}")
        ids[nid] = ou
        tr = n.get("trigger")
        if tr is not None:
            if tr.get("family") not in FAMILLES:
                err(f"{ou} : family de trigger invalide {tr.get('family')!r}")
            if tr.get("family") == "reflex" and "on" not in tr:
                err(f"{ou} : trigger reflex sans 'on'")
        verifier_effets(n.get("effects", []), ou)

    for t in d.get("tiers", []):
        lvl = t.get("level")
        niveaux.append(lvl)
        noeuds = t.get("nodes", [])
        if not noeuds:
            err(f"palier {lvl} : aucun nœud")
        if len(noeuds) > 1 and not t.get("exclusive"):
            err(f"palier {lvl} : {len(noeuds)} nœuds sans exclusive=true")
        if len(noeuds) == 1 and t.get("exclusive"):
            err(f"palier {lvl} : exclusive=true avec un seul nœud")
        if lvl is not None and lvl > d.get("max_level", 50):
            err(f"palier {lvl} au-delà de max_level")
        for n in noeuds:
            verifier_noeud(n, f"palier {lvl}")

    if niveaux != sorted(niveaux):
        err(f"paliers dans le désordre : {niveaux}")

    for b in d.get("branches", []):
        for cle in ("id", "name", "cause", "threshold", "effects"):
            if cle not in b:
                err(f"branche {b.get('id')!r} : sans {cle}")
        verifier_noeud({**b, "description": b.get("description", "")}, f"branche {b.get('id')}")

    causes = {c["id"] for c in d.get("counters", {}).get("per_cause", [])}
    for b in d.get("branches", []):
        if causes and b.get("cause") not in causes:
            err(f"branche {b.get('id')!r} : cause {b.get('cause')!r} absente de counters")

    # renvois entre nœuds
    for t in d.get("tiers", []):
        for n in t.get("nodes", []):
            req = n.get("requires_node")
            if req and req not in ids:
                err(f"nœud {n['id']!r} : requires_node {req!r} introuvable")

    return erreurs


def main(argv):
    dossier = pathlib.Path(__file__).resolve().parent
    cibles = [pathlib.Path(a) for a in argv[1:]] or sorted(dossier.glob("*.json"))
    total = 0
    for c in cibles:
        erreurs = valider(c)
        total += len(erreurs)
        if erreurs:
            print(f"✗ {c.name}")
            for e in erreurs:
                print(f"    {e}")
        else:
            d = json.loads(c.read_text(encoding="utf-8"))
            n = sum(len(t["nodes"]) for t in d["tiers"])
            extra = f" · {len(d['branches'])} branches" if "branches" in d else ""
            print(f"✓ {c.name} — {len(d['tiers'])} paliers · {n} nœuds{extra}")
    print()
    print(f"{len(cibles)} fichiers, {total} erreur(s)")
    return 1 if total else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
