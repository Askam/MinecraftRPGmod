package fr.needaname.mc_rpg_mod.effect.server;

import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Géométrie des quatre formes de {@code break_shape} : {@code vein}, {@code tree}, {@code area},
 * {@code column}. Calcul pur (positions seules, un oracle {@link Predicate} dit ce qui est
 * cassable), testable sans Minecraft. Aucune valeur d'équilibrage : {@code max_blocks} et la taille
 * viennent du barème. L'exécution (casser, butin, endurance, maintien) est à part.
 *
 * <p>Les positions renvoyées incluent l'origine (le bloc miné). L'appelant casse le reste ; l'origine
 * est déjà cassée par le jeu.
 */
public final class BreakShape {
    public enum Shape {
        VEIN, TREE, AREA, COLUMN;

        public static Shape byId(String id) {
            return switch (id) {
                case "vein" -> VEIN;
                case "tree" -> TREE;
                case "area" -> AREA;
                case "column" -> COLUMN;
                default -> throw new IllegalArgumentException("forme inconnue : " + id);
            };
        }
    }

    private BreakShape() {
    }

    public static List<BlockPos> compute(Shape shape, BlockPos origin, OptionalInt maxBlocks,
                                         int[] size, Predicate<BlockPos> breakable) {
        return switch (shape) {
            // Veinage et Abattage sont le même remplissage par contiguïté (26-voisinage) ; seul
            // l'oracle diffère (même minerai / bûches). Le nuancier d'Abattage (monter plutôt que
            // s'étaler sur un arbre voisin) reste à affiner en jeu.
            case VEIN, TREE -> floodFill(origin, maxBlocks.orElse(64), breakable);
            case COLUMN -> column(origin, maxBlocks.orElse(1), breakable);
            case AREA -> area(origin, size, maxBlocks, breakable);
        };
    }

    private static List<BlockPos> floodFill(BlockPos origin, int cap, Predicate<BlockPos> breakable) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin.immutable());
        seen.add(origin.immutable());
        while (!queue.isEmpty() && result.size() < cap) {
            BlockPos pos = queue.poll();
            if (!breakable.test(pos)) {
                continue;
            }
            result.add(pos);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos next = pos.offset(dx, dy, dz);
                        if (seen.add(next)) {
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return result;
    }

    private static List<BlockPos> column(BlockPos origin, int cap, Predicate<BlockPos> breakable) {
        List<BlockPos> result = new ArrayList<>();
        for (int i = 0; i < cap; i++) {
            BlockPos pos = origin.above(i);
            if (!breakable.test(pos)) {
                break; // colonne contiguë vers le haut
            }
            result.add(pos);
        }
        return result;
    }

    private static List<BlockPos> area(BlockPos origin, int[] size, OptionalInt maxBlocks, Predicate<BlockPos> breakable) {
        int sx = size.length > 0 ? size[0] : 1;
        int sy = size.length > 1 ? size[1] : 1;
        int sz = size.length > 2 ? size[2] : 1;
        int rx = sx / 2;
        int rz = sz / 2;
        int cap = maxBlocks.orElse(Integer.MAX_VALUE);
        List<BlockPos> result = new ArrayList<>();
        for (int dy = 0; dy < sy && result.size() < cap; dy++) {
            for (int dx = -rx; dx <= rx && result.size() < cap; dx++) {
                for (int dz = -rz; dz <= rz && result.size() < cap; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (breakable.test(pos)) {
                        result.add(pos);
                    }
                }
            }
        }
        return result;
    }
}
