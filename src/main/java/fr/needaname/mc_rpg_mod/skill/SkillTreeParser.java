package fr.needaname.mc_rpg_mod.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.needaname.mc_rpg_mod.skill.model.Appearance;
import fr.needaname.mc_rpg_mod.skill.model.AttributeOperation;
import fr.needaname.mc_rpg_mod.skill.model.Branch;
import fr.needaname.mc_rpg_mod.skill.model.CauseCounter;
import fr.needaname.mc_rpg_mod.skill.model.Counters;
import fr.needaname.mc_rpg_mod.skill.model.Effect;
import fr.needaname.mc_rpg_mod.skill.model.EffectType;
import fr.needaname.mc_rpg_mod.skill.model.Filter;
import fr.needaname.mc_rpg_mod.skill.model.Node;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import fr.needaname.mc_rpg_mod.skill.model.Tier;
import fr.needaname.mc_rpg_mod.skill.model.Trigger;
import fr.needaname.mc_rpg_mod.skill.model.TriggerFamily;
import fr.needaname.mc_rpg_mod.skill.model.XpCurve;
import fr.needaname.mc_rpg_mod.skill.model.XpOnDeath;
import fr.needaname.mc_rpg_mod.skill.model.XpSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * JSON → {@link SkillTree}. Pur Gson, sans aucune dépendance Minecraft : testable sans
 * {@code Bootstrap}. Applique au minimum les règles de {@code tools/valider.py} et refuse tout
 * {@code type} d'effet hors catalogue ou {@code schema} inattendu, en nommant fichier et champ.
 *
 * <p>Fail-fast : la première erreur rencontrée refuse le fichier. Les ids/tags restent des chaînes
 * brutes, résolus en {@code TagKey}/{@code ResourceKey} dans une étape séparée.
 */
public final class SkillTreeParser {
    private static final int SUPPORTED_SCHEMA = 1;
    private static final int DEFAULT_MAX_LEVEL = 50;

    private SkillTreeParser() {
    }

    /**
     * @param file le radical du fichier (ex. {@code "agilite"}), qui doit être égal au champ {@code id}
     * @throws SkillLoadException si le barème est invalide
     */
    public static SkillTree parse(String file, JsonObject root) {
        // Le schéma d'abord : un numéro inconnu refuse le fichier avant toute interprétation.
        int schema = reqInt(file, "schema", root, "schema");
        if (schema != SUPPORTED_SCHEMA) {
            throw err(file, "schema", "version inconnue " + schema + " (attendu " + SUPPORTED_SCHEMA + ")");
        }

        String id = reqString(file, "id", root, "id");
        if (!id.equals(file)) {
            throw err(file, "id", "'" + id + "' ne correspond pas au nom de fichier '" + file + "'");
        }
        String name = reqString(file, "name", root, "name");
        String color = reqString(file, "color", root, "color");
        int maxLevel = reqInt(file, "max_level", root, "max_level");
        XpCurve curve = parseCurve(file, reqObject(file, "xp_curve", root, "xp_curve"));

        boolean hasXpSources = root.has("xp_sources");
        boolean hasXpOnDeath = root.has("xp_on_death");
        if (!hasXpSources && !hasXpOnDeath) {
            throw err(file, "racine", "ni xp_sources ni xp_on_death");
        }

        List<XpSource> xpSources = new ArrayList<>();
        if (hasXpSources) {
            JsonArray arr = reqArray(file, "xp_sources", root, "xp_sources");
            for (int i = 0; i < arr.size(); i++) {
                String where = "xp_sources[" + i + "]";
                xpSources.add(parseXpSource(file, where, asObject(file, where, arr.get(i))));
            }
        }

        Optional<XpOnDeath> xpOnDeath = Optional.empty();
        if (hasXpOnDeath) {
            JsonObject o = reqObject(file, "xp_on_death", root, "xp_on_death");
            xpOnDeath = Optional.of(new XpOnDeath(
                    reqInt(file, "xp_on_death.flat", o, "flat"),
                    reqInt(file, "xp_on_death.requires_active_seconds_since_last_death", o,
                            "requires_active_seconds_since_last_death")));
        }

        Optional<Counters> counters = Optional.empty();
        Set<String> causeIds = new LinkedHashSet<>();
        if (root.has("counters")) {
            JsonObject o = reqObject(file, "counters", root, "counters");
            boolean total = optBoolean(o, "total", false);
            List<CauseCounter> perCause = new ArrayList<>();
            if (o.has("per_cause")) {
                JsonArray arr = reqArray(file, "counters.per_cause", o, "per_cause");
                for (int i = 0; i < arr.size(); i++) {
                    String where = "counters.per_cause[" + i + "]";
                    JsonObject c = asObject(file, where, arr.get(i));
                    String causeId = reqString(file, where, c, "id");
                    if (!causeIds.add(causeId)) {
                        throw err(file, where, "id de cause en double '" + causeId + "'");
                    }
                    perCause.add(new CauseCounter(causeId,
                            optString(c, "damage_type"),
                            optString(c, "damage_type_tag"),
                            optString(c, "source_entity"),
                            optString(c, "excluding_source_entity")));
                }
            }
            counters = Optional.of(new Counters(total, perCause));
        }

        // Ids de tous les nœuds (paliers + branches) : unicité globale, et cible de requires_node.
        Set<String> allNodeIds = new LinkedHashSet<>();

        List<Tier> tiers = parseTiers(file, reqArray(file, "tiers", root, "tiers"), maxLevel, allNodeIds);
        List<Branch> branches = parseBranches(file, root, causeIds, allNodeIds);
        List<Appearance> appearances = parseAppearances(file, root);

        // requires_node : vérifié une fois tous les ids connus (il peut viser un nœud d'un autre palier).
        for (Tier tier : tiers) {
            for (Node node : tier.nodes()) {
                if (node.requiresNode().isPresent() && !allNodeIds.contains(node.requiresNode().get())) {
                    throw err(file, "nœud " + node.id(),
                            "requires_node '" + node.requiresNode().get() + "' introuvable");
                }
            }
        }

        return new SkillTree(schema, id, name, color, maxLevel, curve,
                xpSources, xpOnDeath, counters, tiers, branches, appearances);
    }

    private static XpCurve parseCurve(String file, JsonObject o) {
        return new XpCurve(
                reqDouble(file, "xp_curve.base", o, "base"),
                reqDouble(file, "xp_curve.exponent", o, "exponent"));
    }

    private static XpSource parseXpSource(String file, String where, JsonObject o) {
        if (o.has("stat")) {
            return new XpSource.Stat(
                    reqString(file, where, o, "stat"),
                    reqInt(file, where, o, "per"),
                    reqInt(file, where, o, "xp"));
        }
        if (o.has("on")) {
            return new XpSource.Event(
                    reqString(file, where, o, "on"),
                    optInt(o, "per"),
                    optFilter(o),
                    optObject(o, "require"),
                    reqInt(file, where, o, "xp"));
        }
        throw err(file, where, "source d'XP sans 'stat' ni 'on'");
    }

    private static List<Tier> parseTiers(String file, JsonArray arr, int maxLevel, Set<String> allNodeIds) {
        List<Tier> tiers = new ArrayList<>();
        int previousLevel = Integer.MIN_VALUE;
        for (int i = 0; i < arr.size(); i++) {
            String where = "palier[" + i + "]";
            JsonObject t = asObject(file, where, arr.get(i));
            int level = reqInt(file, where, t, "level");
            where = "palier " + level;
            boolean exclusive = optBoolean(t, "exclusive", false);

            JsonArray nodesArr = reqArray(file, where, t, "nodes");
            if (nodesArr.isEmpty()) {
                throw err(file, where, "aucun nœud");
            }
            if (nodesArr.size() > 1 && !exclusive) {
                throw err(file, where, nodesArr.size() + " nœuds sans exclusive=true");
            }
            if (nodesArr.size() == 1 && exclusive) {
                throw err(file, where, "exclusive=true avec un seul nœud");
            }
            if (level > maxLevel) {
                throw err(file, where, "niveau au-delà de max_level (" + maxLevel + ")");
            }
            if (level < previousLevel) {
                throw err(file, where, "paliers dans le désordre (après le niveau " + previousLevel + ")");
            }
            previousLevel = level;

            List<Node> nodes = new ArrayList<>();
            for (int j = 0; j < nodesArr.size(); j++) {
                nodes.add(parseNode(file, where, asObject(file, where, nodesArr.get(j)), allNodeIds));
            }
            tiers.add(new Tier(level, exclusive, nodes));
        }
        return tiers;
    }

    private static Node parseNode(String file, String where, JsonObject n, Set<String> allNodeIds) {
        String id = reqString(file, where, n, "id");
        if (!allNodeIds.add(id)) {
            throw err(file, where, "id de nœud en double '" + id + "'");
        }
        String nodeWhere = "nœud " + id;
        String name = reqString(file, nodeWhere, n, "name");
        String description = reqString(file, nodeWhere, n, "description");
        Optional<Trigger> trigger = parseTrigger(file, nodeWhere, n);
        List<Effect> effects = parseEffects(file, nodeWhere, reqArray(file, nodeWhere, n, "effects"));
        Optional<String> requiresNode = optString(n, "requires_node");
        return new Node(id, name, description, trigger, effects, requiresNode);
    }

    private static List<Branch> parseBranches(String file, JsonObject root, Set<String> causeIds, Set<String> allNodeIds) {
        if (!root.has("branches")) {
            return List.of();
        }
        JsonArray arr = reqArray(file, "branches", root, "branches");
        List<Branch> branches = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            String where = "branche[" + i + "]";
            JsonObject b = asObject(file, where, arr.get(i));
            String id = reqString(file, where, b, "id");
            if (!allNodeIds.add(id)) {
                throw err(file, where, "id de branche en double '" + id + "'");
            }
            String branchWhere = "branche " + id;
            String name = reqString(file, branchWhere, b, "name");
            String cause = reqString(file, branchWhere, b, "cause");
            int threshold = reqInt(file, branchWhere, b, "threshold");
            if (!causeIds.isEmpty() && !causeIds.contains(cause)) {
                throw err(file, branchWhere, "cause '" + cause + "' absente de counters.per_cause");
            }
            String description = optString(b, "description").orElse("");
            Optional<Trigger> trigger = parseTrigger(file, branchWhere, b);
            List<Effect> effects = parseEffects(file, branchWhere, reqArray(file, branchWhere, b, "effects"));
            branches.add(new Branch(id, name, description, cause, threshold, trigger, effects));
        }
        return branches;
    }

    private static List<Appearance> parseAppearances(String file, JsonObject root) {
        if (!root.has("appearances")) {
            return List.of();
        }
        JsonArray arr = reqArray(file, "appearances", root, "appearances");
        List<Appearance> appearances = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            String where = "appearances[" + i + "]";
            JsonObject a = asObject(file, where, arr.get(i));
            appearances.add(new Appearance(
                    reqInt(file, where, a, "at_level"),
                    reqString(file, where, a, "layer"),
                    a));
        }
        return appearances;
    }

    private static Optional<Trigger> parseTrigger(String file, String where, JsonObject owner) {
        if (!owner.has("trigger")) {
            return Optional.empty();
        }
        JsonObject t = reqObject(file, where + ".trigger", owner, "trigger");
        String familyStr = reqString(file, where + ".trigger", t, "family");
        TriggerFamily family = TriggerFamily.byId(familyStr)
                .orElseThrow(() -> err(file, where + ".trigger", "family invalide '" + familyStr + "'"));
        Optional<String> on = optString(t, "on");
        if (family == TriggerFamily.REFLEX && on.isEmpty()) {
            throw err(file, where + ".trigger", "trigger reflex sans 'on'");
        }
        return Optional.of(new Trigger(family, on, optObject(t, "require")));
    }

    private static List<Effect> parseEffects(String file, String where, JsonArray arr) {
        List<Effect> effects = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            String effectWhere = where + ".effects[" + i + "]";
            JsonObject e = asObject(file, effectWhere, arr.get(i));
            String typeStr = reqString(file, effectWhere, e, "type");
            EffectType type = EffectType.byId(typeStr)
                    .orElseThrow(() -> err(file, effectWhere, "type d'effet inconnu '" + typeStr + "'"));

            if (type == EffectType.ATTRIBUTE) {
                String op = reqString(file, effectWhere, e, "operation");
                if (AttributeOperation.byId(op).isEmpty()) {
                    throw err(file, effectWhere, "operation invalide '" + op + "'");
                }
            }
            // Un effet qui en corrige un autre (overrides) hérite des champs qu'il ne redit pas, cooldown compris.
            if (type == EffectType.ABILITY && !e.has("cooldown_ticks") && !e.has("overrides")) {
                throw err(file, effectWhere, "ability sans cooldown_ticks (mettre 0 si aucune)");
            }
            effects.add(new Effect(type, e));
        }
        return effects;
    }

    // --- Lecteurs JSON : chaque échec nomme fichier + champ. ---

    private static SkillLoadException err(String file, String where, String detail) {
        return new SkillLoadException(file + ".json : " + where + " : " + detail);
    }

    private static JsonObject asObject(String file, String where, JsonElement el) {
        if (!el.isJsonObject()) {
            throw err(file, where, "objet JSON attendu");
        }
        return el.getAsJsonObject();
    }

    private static JsonElement req(String file, String where, JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            throw err(file, where, "champ requis manquant '" + key + "'");
        }
        return o.get(key);
    }

    private static String reqString(String file, String where, JsonObject o, String key) {
        JsonElement el = req(file, where, o, key);
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
            throw err(file, where, "chaîne attendue pour '" + key + "'");
        }
        return el.getAsString();
    }

    private static int reqInt(String file, String where, JsonObject o, String key) {
        JsonElement el = req(file, where, o, key);
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            throw err(file, where, "entier attendu pour '" + key + "'");
        }
        return el.getAsInt();
    }

    private static double reqDouble(String file, String where, JsonObject o, String key) {
        JsonElement el = req(file, where, o, key);
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            throw err(file, where, "nombre attendu pour '" + key + "'");
        }
        return el.getAsDouble();
    }

    private static JsonObject reqObject(String file, String where, JsonObject o, String key) {
        JsonElement el = req(file, where, o, key);
        if (!el.isJsonObject()) {
            throw err(file, where, "objet attendu pour '" + key + "'");
        }
        return el.getAsJsonObject();
    }

    private static JsonArray reqArray(String file, String where, JsonObject o, String key) {
        JsonElement el = req(file, where, o, key);
        if (!el.isJsonArray()) {
            throw err(file, where, "tableau attendu pour '" + key + "'");
        }
        return el.getAsJsonArray();
    }

    private static Optional<String> optString(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            return Optional.empty();
        }
        return Optional.of(o.get(key).getAsString());
    }

    private static Optional<Integer> optInt(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            return Optional.empty();
        }
        return Optional.of(o.get(key).getAsInt());
    }

    private static boolean optBoolean(JsonObject o, String key, boolean fallback) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            return fallback;
        }
        return o.get(key).getAsBoolean();
    }

    private static Optional<JsonObject> optObject(JsonObject o, String key) {
        if (!o.has(key) || !o.get(key).isJsonObject()) {
            return Optional.empty();
        }
        return Optional.of(o.get(key).getAsJsonObject());
    }

    private static Optional<Filter> optFilter(JsonObject o) {
        if (!o.has("filter") || o.get("filter").isJsonNull()) {
            return Optional.empty();
        }
        JsonElement el = o.get("filter");
        List<String> entries = new ArrayList<>();
        if (el.isJsonArray()) {
            el.getAsJsonArray().forEach(e -> entries.add(e.getAsString()));
        } else {
            entries.add(el.getAsString());
        }
        return Optional.of(new Filter(entries));
    }
}
