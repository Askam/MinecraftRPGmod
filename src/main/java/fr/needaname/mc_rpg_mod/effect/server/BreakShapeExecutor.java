package fr.needaname.mc_rpg_mod.effect.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.effect.ActiveEffect;
import fr.needaname.mc_rpg_mod.effect.Filters;
import fr.needaname.mc_rpg_mod.effect.ServerEffects;
import fr.needaname.mc_rpg_mod.skill.model.EffectType;
import fr.needaname.mc_rpg_mod.skill.model.Filter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

/**
 * Exécute {@code break_shape} (Veinage, Abattage, Moisson, Carrier) quand le joueur casse un bloc
 * <b>en maintenant</b> la touche de récolte ({@link HoldKeyState}, alimenté par paquet au bloc 8).
 * L'origine est cassée par le jeu ; on casse le reste de la forme calculée par {@link BreakShape}.
 *
 * <p>Toujours côté serveur, après vérification de la possession du nœud (via {@link ServerEffects}).
 * Un garde anti-réentrance évite toute cascade. {@code also_breaks} casse en plus les blocs voisins
 * d'un autre filtre (les feuilles pour Abattage).
 *
 * <p>À vérifier en jeu (impossible sans client) : le ressenti, l'orientation d'Abattage, et surtout
 * {@code stamina_per_block} — il n'existe pas encore de réserve d'endurance dans le mod, donc ce coût
 * n'est PAS prélevé pour l'instant (point ouvert). {@code same_block_only} et {@code mature_only} :
 * seul le premier est appliqué ; la maturité des cultures reste à gérer.
 */
@Mod.EventBusSubscriber(modid = RpgMod.MODID)
public final class BreakShapeExecutor {
    private static final ThreadLocal<Boolean> RUNNING = ThreadLocal.withInitial(() -> false);
    private static final int ALSO_BREAKS_LIMIT = 256;

    private BreakShapeExecutor() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (RUNNING.get()) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!HoldKeyState.isHeld(player)) {
            return;
        }
        BlockPos origin = event.getPos();
        BlockState originState = event.getState();

        for (ActiveEffect active : ServerEffects.active(player, EffectType.BREAK_SHAPE)) {
            JsonObject fields = active.effect().fields();
            Optional<Filter> filter = Filters.of(fields, "filter");
            if (filter.isPresent() && !Filters.matchesBlock(filter.get(), originState)) {
                continue;
            }
            runShape(player, level, origin, originState, fields, filter.orElse(null));
        }
    }

    private static void runShape(ServerPlayer player, ServerLevel level, BlockPos origin,
                                 BlockState originState, JsonObject fields, Filter filter) {
        BreakShape.Shape shape;
        try {
            shape = BreakShape.Shape.byId(fields.get("shape").getAsString());
        } catch (RuntimeException ex) {
            return;
        }
        boolean sameBlockOnly = fields.has("same_block_only") && fields.get("same_block_only").getAsBoolean();
        Block originBlock = originState.getBlock();

        Predicate<BlockPos> breakable = pos -> {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                return false;
            }
            if (sameBlockOnly && !state.is(originBlock)) {
                return false;
            }
            return filter == null || Filters.matchesBlock(filter, state);
        };

        List<BlockPos> positions = BreakShape.compute(shape, origin, maxBlocks(fields), size(fields), breakable);

        RUNNING.set(true);
        try {
            for (BlockPos pos : positions) {
                if (!pos.equals(origin)) {
                    level.destroyBlock(pos, true, player);
                }
            }
            Filters.of(fields, "also_breaks").ifPresent(extra -> breakAlso(level, player, positions, extra));
        } finally {
            RUNNING.set(false);
        }
    }

    /** Casse en plus les blocs d'un autre filtre voisins des blocs déjà cassés (feuilles d'Abattage). */
    private static void breakAlso(ServerLevel level, ServerPlayer player, List<BlockPos> broken, Filter extraFilter) {
        int budget = ALSO_BREAKS_LIMIT;
        java.util.Set<BlockPos> done = new java.util.HashSet<>();
        for (BlockPos base : broken) {
            for (int dx = -1; dx <= 1 && budget > 0; dx++) {
                for (int dy = -1; dy <= 1 && budget > 0; dy++) {
                    for (int dz = -1; dz <= 1 && budget > 0; dz++) {
                        BlockPos pos = base.offset(dx, dy, dz);
                        if (!done.add(pos)) {
                            continue;
                        }
                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir() && Filters.matchesBlock(extraFilter, state)) {
                            level.destroyBlock(pos, true, player);
                            budget--;
                        }
                    }
                }
            }
        }
    }

    private static OptionalInt maxBlocks(JsonObject fields) {
        return fields.has("max_blocks") ? OptionalInt.of(fields.get("max_blocks").getAsInt()) : OptionalInt.empty();
    }

    private static int[] size(JsonObject fields) {
        if (!fields.has("size") || !fields.get("size").isJsonArray()) {
            return new int[]{1, 1, 1};
        }
        JsonArray array = fields.getAsJsonArray("size");
        int[] size = new int[]{1, 1, 1};
        for (int i = 0; i < size.length && i < array.size(); i++) {
            size[i] = array.get(i).getAsInt();
        }
        return size;
    }
}
