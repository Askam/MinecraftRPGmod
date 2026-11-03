package fr.needaname.mc_rpg_mod.death;

import fr.needaname.mc_rpg_mod.RpgMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RpgMod.MODID)
public final class DeathCapabilityEvents {
    private DeathCapabilityEvents() {}

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            // Pas de event.addListener(provider::invalidate) : invalidateCaps() à la mort invaliderait
            // définitivement le LazyOptional, et reviveCaps() dans onClone ne pourrait plus le ranimer.
            event.addCapability(DeathStatsProvider.ID, new DeathStatsProvider());
        }
    }

    /**
     * Le joueur est recréé au respawn (isWasDeath) ET au retour de l'End (!isWasDeath) :
     * dans les deux cas la capability neuve est vide, donc on recopie toujours.
     * Le LivingDeathEvent a déjà incrémenté l'ancienne instance, la mort en cours est donc conservée.
     */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        // En 1.20.1 les caps de l'ancien joueur sont invalidées avant Clone : il faut les ranimer le temps de la copie.
        original.reviveCaps();
        DeathStats.get(original).ifPresent(old ->
                DeathStats.get(event.getEntity()).ifPresent(fresh -> fresh.copyFrom(old)));
        original.invalidateCaps();
    }
}
