package fr.needaname.mc_rpg_mod.effect.server;

import com.google.gson.JsonObject;
import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.effect.ActiveEffect;
import fr.needaname.mc_rpg_mod.effect.Filters;
import fr.needaname.mc_rpg_mod.effect.ServerEffects;
import fr.needaname.mc_rpg_mod.skill.model.EffectType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

/**
 * Effet {@code drop_bonus} : butin supplémentaire sur un tag de blocs, avec une chance. {@code item}
 * fait tomber autre chose que le bloc (la résine de Sève).
 *
 * <p>Gère un item absent du registre sans planter : {@code mc_rpg_mod:resine} n'est pas encore
 * enregistré (point ouvert), donc on journalise en debug et on n'ajoute rien, au lieu de crasher.
 *
 * <p>Nuances {@code seeds_only} / {@code mature_only} non interprétées ici (à affiner en jeu).
 */
@Mod.EventBusSubscriber(modid = RpgMod.MODID)
public final class DropBonusEffects {
    private DropBonusEffects() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = event.getState();
        for (ActiveEffect active : ServerEffects.active(player, EffectType.DROP_BONUS)) {
            JsonObject fields = active.effect().fields();
            Optional<fr.needaname.mc_rpg_mod.skill.model.Filter> filter = Filters.of(fields, "filter");
            if (filter.isPresent() && !Filters.matchesBlock(filter.get(), state)) {
                continue;
            }
            double chance = fields.has("chance") ? fields.get("chance").getAsDouble() : 1.0;
            if (player.getRandom().nextDouble() > chance) {
                continue;
            }
            int extra = fields.has("extra") ? fields.get("extra").getAsInt() : 1;
            ItemStack drop = resolveDrop(fields, state, active.ownerId());
            if (drop == null || drop.isEmpty() || extra <= 0) {
                continue;
            }
            drop.setCount(extra);
            Block.popResource(level, event.getPos(), drop);
        }
    }

    private static ItemStack resolveDrop(JsonObject fields, BlockState state, String ownerId) {
        if (fields.has("item")) {
            ResourceLocation itemId = ResourceLocation.tryParse(fields.get("item").getAsString());
            if (itemId == null) {
                return null;
            }
            Optional<Item> item = BuiltInRegistries.ITEM.getOptional(itemId);
            if (item.isEmpty()) {
                // Item non encore enregistré (ex. mc_rpg_mod:resine) : on ne plante pas, on n'ajoute rien.
                RpgMod.LOGGER.debug("drop_bonus {} : item absent du registre {}, butin ignoré", ownerId, itemId);
                return null;
            }
            return new ItemStack(item.get());
        }
        return new ItemStack(state.getBlock());
    }
}
