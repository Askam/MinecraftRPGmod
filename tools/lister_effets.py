#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Liste les actions (type ability) et les drapeaux (type flag) des barèmes.

    python3 lister_effets.py                 # tous les .json du dossier competences
    python3 lister_effets.py a.json b.json

Sert à cadrer le bloc 6 : combien d'actions et de drapeaux à implémenter, et
lesquels. Un drapeau porteur d'un champ « multiplier » est signalé « (mult) » :
ce sont les candidats au repli éventuel en type `tweak` (décision du propriétaire).
"""
import json
import pathlib
import sys


def effects_of(node):
    return node.get("effects", [])


def walk_effects(data):
    for tier in data.get("tiers", []):
        for node in tier.get("nodes", []):
            yield from effects_of(node)
    for branch in data.get("branches", []):
        yield from effects_of(branch)


def main(argv):
    dossier = pathlib.Path(__file__).resolve().parent.parent \
        / "src/main/resources/data/mc_rpg_mod/competences"
    cibles = [pathlib.Path(a) for a in argv[1:]] or sorted(dossier.glob("*.json"))

    actions = {}   # action -> nombre d'occurrences
    flags = {}     # flag -> {"count": n, "mult": bool}
    for chemin in cibles:
        data = json.loads(chemin.read_text(encoding="utf-8"))
        for effect in walk_effects(data):
            t = effect.get("type")
            if t == "ability":
                action = effect.get("action", "?")
                actions[action] = actions.get(action, 0) + 1
            elif t == "flag":
                flag = effect.get("flag", "?")
                entry = flags.setdefault(flag, {"count": 0, "mult": False})
                entry["count"] += 1
                if "multiplier" in effect:
                    entry["mult"] = True

    print(f"Actions (type ability) : {len(actions)}")
    for action in sorted(actions):
        print(f"  {action} ×{actions[action]}")
    mult_count = sum(1 for f in flags.values() if f["mult"])
    print(f"\nDrapeaux (type flag) : {len(flags)}  "
          f"(dont {mult_count} porteurs d'un multiplier)")
    for flag in sorted(flags):
        mark = " (mult)" if flags[flag]["mult"] else ""
        print(f"  {flag} ×{flags[flag]['count']}{mark}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
