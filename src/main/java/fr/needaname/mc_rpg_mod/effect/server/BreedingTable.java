package fr.needaname.mc_rpg_mod.effect.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Table de poids de l'effet {@code breeding} : {@code [{ weight, children }]}. Un seul tirage.
 * Logique pure (aucune valeur d'équilibrage ici : poids et nombres viennent du barème), testable
 * sans Minecraft. Le serveur tire {@code children(random.nextInt(totalWeight()))}.
 */
public final class BreedingTable {
    public record Row(int weight, int children) {
    }

    private final List<Row> rows;
    private final int totalWeight;

    public BreedingTable(List<Row> rows) {
        this.rows = List.copyOf(rows);
        int sum = 0;
        for (Row row : rows) {
            sum += Math.max(0, row.weight());
        }
        this.totalWeight = sum;
    }

    public static BreedingTable fromJson(JsonArray table) {
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < table.size(); i++) {
            JsonObject row = table.get(i).getAsJsonObject();
            rows.add(new Row(row.get("weight").getAsInt(), row.get("children").getAsInt()));
        }
        return new BreedingTable(rows);
    }

    public int totalWeight() {
        return totalWeight;
    }

    /**
     * Nombre de petits pour un point de poids tiré dans {@code [0, totalWeight())}.
     * Déterministe : c'est la fonction testée ; le hasard reste à l'appelant.
     */
    public int children(int weightPoint) {
        int cursor = 0;
        for (Row row : rows) {
            cursor += Math.max(0, row.weight());
            if (weightPoint < cursor) {
                return row.children();
            }
        }
        return rows.isEmpty() ? 1 : rows.get(rows.size() - 1).children();
    }
}
