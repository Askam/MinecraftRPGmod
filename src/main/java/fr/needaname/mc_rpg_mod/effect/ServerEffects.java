package fr.needaname.mc_rpg_mod.effect;

import fr.needaname.mc_rpg_mod.category.CategorySavedData;
import fr.needaname.mc_rpg_mod.death.DeathStats;
import fr.needaname.mc_rpg_mod.skill.SkillTreeManager;
import fr.needaname.mc_rpg_mod.skill.model.EffectType;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import fr.needaname.mc_rpg_mod.skill.player.PlayerSkills;
import fr.needaname.mc_rpg_mod.skill.player.PlayerSkillsProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Accès central des effets serveur : « quels effets de tel type ce joueur possède-t-il, tous arbres
 * confondus ? ». Combine la progression ({@link PlayerSkills}) et les compteurs de mort
 * ({@link DeathStats}, pour les branches) via {@link EffectResolver}. Chaque hook d'effet (dégâts,
 * chute, butin, attributs…) part d'ici.
 */
public final class ServerEffects {
    private ServerEffects() {
    }

    public static List<ActiveEffect> active(ServerPlayer player, EffectType type) {
        PlayerSkills skills = PlayerSkillsProvider.get(player).orElse(null);
        if (skills == null) {
            return List.of();
        }
        ToIntFunction<String> causeCount = DeathStats.get(player)
                .<ToIntFunction<String>>map(stats -> stats::getCount)
                .orElse(cause -> 0);
        // Un arbre désactivé par l'administrateur ne produit plus d'effets (reapply fait le retrait).
        CategorySavedData categories = player.getServer() == null ? null : CategorySavedData.get(player.getServer());

        List<ActiveEffect> out = new ArrayList<>();
        for (SkillTree tree : SkillTreeManager.INSTANCE.trees().values()) {
            if (categories != null && categories.isDisabled(tree.id())) {
                continue;
            }
            EffectResolver.Progress progress =
                    EffectResolver.progress(skills.level(tree.id()), skills.nodes(), causeCount);
            out.addAll(EffectResolver.activeOfType(tree, progress, type));
        }
        return out;
    }
}
