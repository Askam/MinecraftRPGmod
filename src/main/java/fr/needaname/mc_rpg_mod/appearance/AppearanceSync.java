package fr.needaname.mc_rpg_mod.appearance;

import fr.needaname.mc_rpg_mod.effect.ActiveEffect;
import fr.needaname.mc_rpg_mod.effect.ServerEffects;
import fr.needaname.mc_rpg_mod.network.RpgNetwork;
import fr.needaname.mc_rpg_mod.network.SyncAppearancePacket;
import fr.needaname.mc_rpg_mod.skill.SkillTreeManager;
import fr.needaname.mc_rpg_mod.skill.model.Appearance;
import fr.needaname.mc_rpg_mod.skill.model.EffectType;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import fr.needaname.mc_rpg_mod.skill.player.PlayerSkills;
import fr.needaname.mc_rpg_mod.skill.player.PlayerSkillsProvider;
import net.minecraft.server.level.ServerPlayer;

/**
 * Calcule les drapeaux d'apparence d'un joueur côté serveur et les envoie (à lui-même et à ceux qui
 * le voient). Pilotés par l'arbre de Mort : aura rouge selon le niveau (clé {@code appearances}),
 * peau de cendre selon la branche active (effet {@code appearance} de calque {@code ash_skin}).
 *
 * <p>L'armure spectrale dépend d'un état transitoire ({@code spectral_armour_active}) non suivi pour
 * l'instant : le drapeau reste faux (point ouvert, à relier à l'ability de respawn en session locale).
 */
public final class AppearanceSync {
    private static final String DEATH_TREE = "mort";

    private AppearanceSync() {
    }

    public static void sendFor(ServerPlayer player) {
        RpgNetwork.sendToTrackingAndSelf(player, compute(player));
    }

    public static void sendTo(ServerPlayer viewer, ServerPlayer target) {
        RpgNetwork.sendTo(viewer, compute(target));
    }

    public static SyncAppearancePacket compute(ServerPlayer player) {
        float aura = 0f;
        boolean ash = false;

        SkillTree mort = SkillTreeManager.INSTANCE.byId(DEATH_TREE).orElse(null);
        PlayerSkills skills = PlayerSkillsProvider.get(player).orElse(null);
        if (mort != null && skills != null) {
            int level = skills.level(DEATH_TREE);
            for (Appearance appearance : mort.appearances()) {
                if (appearance.layer().equals("red_aura") && level >= appearance.atLevel()) {
                    float intensity = appearance.fields().has("intensity")
                            ? appearance.fields().get("intensity").getAsFloat() : 1f;
                    aura = Math.max(aura, intensity);
                }
            }
            for (ActiveEffect active : ServerEffects.active(player, EffectType.APPEARANCE)) {
                if (active.effect().fields().has("layer")
                        && "ash_skin".equals(active.effect().fields().get("layer").getAsString())) {
                    ash = true;
                }
            }
        }
        return new SyncAppearancePacket(player.getId(), aura, ash, false);
    }
}
