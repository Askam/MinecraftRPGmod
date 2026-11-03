package fr.needaname.mc_rpg_mod.skill.player;

import fr.needaname.mc_rpg_mod.RpgMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Attache {@link PlayerSkillsProvider} aux joueurs et recopie les données au respawn / retour de
 * dimension, exactement comme {@code DeathCapabilityEvents} le fait pour les compteurs de mort :
 * {@code reviveCaps()} avant la copie, {@code invalidateCaps()} après, et jamais de listener
 * d'invalidation à l'attache (sinon le {@link net.minecraftforge.common.util.LazyOptional} serait
 * invalidé définitivement et les données perdues à la première mort).
 *
 * <p>Gestionnaire distinct de celui de la mort : chaque {@code onClone} encadre entièrement son
 * propre accès (revive → copie → invalidate), donc les deux cohabitent sans interférer.
 */
@Mod.EventBusSubscriber(modid = RpgMod.MODID)
public final class SkillCapabilityEvents {
    private SkillCapabilityEvents() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(PlayerSkillsProvider.ID, new PlayerSkillsProvider());
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        PlayerSkillsProvider.get(original).ifPresent(old ->
                PlayerSkillsProvider.get(event.getEntity()).ifPresent(fresh -> fresh.copyFrom(old)));
        original.invalidateCaps();
    }
}
