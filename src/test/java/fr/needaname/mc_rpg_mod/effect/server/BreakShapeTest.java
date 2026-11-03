package fr.needaname.mc_rpg_mod.effect.server;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Géométrie des formes : pure, testée sans Minecraft via un oracle de positions cassables. */
class BreakShapeTest {

    private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);

    @Test
    void veinFloodFillRespectsConnectivityAndCap() {
        // Un amas de 12 positions contiguës (colonne + voisins) ; cap 8.
        Set<BlockPos> ore = new HashSet<>();
        for (int i = 0; i < 12; i++) {
            ore.add(ORIGIN.above(i));
        }
        List<BlockPos> broken = BreakShape.compute(BreakShape.Shape.VEIN, ORIGIN, OptionalInt.of(8),
                null, ore::contains);
        assertEquals(8, broken.size());
        assertTrue(broken.contains(ORIGIN));
        assertTrue(ore.containsAll(broken));
    }

    @Test
    void veinStopsAtNonMatchingNeighbours() {
        // Seules l'origine et deux voisins au-dessus sont du minerai ; le reste n'est pas cassable.
        Set<BlockPos> ore = Set.of(ORIGIN, ORIGIN.above(1), ORIGIN.above(2));
        List<BlockPos> broken = BreakShape.compute(BreakShape.Shape.VEIN, ORIGIN, OptionalInt.of(8),
                null, ore::contains);
        assertEquals(3, broken.size());
    }

    @Test
    void columnGoesUpAndStopsAtGap() {
        Set<BlockPos> stone = Set.of(ORIGIN, ORIGIN.above(1), ORIGIN.above(2), ORIGIN.above(4));
        // max 3 → origine + 2 au-dessus ; s'arrête avant le trou à +3 de toute façon.
        List<BlockPos> broken = BreakShape.compute(BreakShape.Shape.COLUMN, ORIGIN, OptionalInt.of(3),
                null, stone::contains);
        assertEquals(List.of(ORIGIN, ORIGIN.above(1), ORIGIN.above(2)), broken);
    }

    @Test
    void columnStopsEarlyOnGap() {
        Set<BlockPos> stone = Set.of(ORIGIN, ORIGIN.above(1)); // trou à +2
        List<BlockPos> broken = BreakShape.compute(BreakShape.Shape.COLUMN, ORIGIN, OptionalInt.of(3),
                null, stone::contains);
        assertEquals(2, broken.size());
    }

    @Test
    void areaIsA3x1x3SquareWhenAllBreakable() {
        List<BlockPos> broken = BreakShape.compute(BreakShape.Shape.AREA, ORIGIN, OptionalInt.empty(),
                new int[]{3, 1, 3}, pos -> pos.getY() == ORIGIN.getY());
        assertEquals(9, broken.size());
        assertTrue(broken.contains(ORIGIN));
        assertTrue(broken.contains(ORIGIN.offset(1, 0, 1)));
        assertTrue(broken.contains(ORIGIN.offset(-1, 0, -1)));
    }

    @Test
    void areaSkipsNonBreakableCells() {
        // Seul un ruban central est mûr : 3 positions.
        List<BlockPos> broken = BreakShape.compute(BreakShape.Shape.AREA, ORIGIN, OptionalInt.empty(),
                new int[]{3, 1, 3}, pos -> pos.getX() == 0 && pos.getY() == ORIGIN.getY());
        assertEquals(3, broken.size());
    }
}
