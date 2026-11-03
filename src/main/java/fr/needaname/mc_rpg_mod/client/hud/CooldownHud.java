package fr.needaname.mc_rpg_mod.client.hud;

import fr.needaname.mc_rpg_mod.effect.server.Cooldowns;
import fr.needaname.mc_rpg_mod.skill.ClientSkillTrees;
import fr.needaname.mc_rpg_mod.skill.model.EffectType;
import fr.needaname.mc_rpg_mod.skill.model.Node;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import fr.needaname.mc_rpg_mod.skill.model.Tier;
import fr.needaname.mc_rpg_mod.skill.player.PlayerSkills;
import fr.needaname.mc_rpg_mod.skill.player.PlayerSkillsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/**
 * HUD des recharges : une rangée de petites tuiles au-dessus de la barre d'objets, visible UNIQUEMENT
 * pendant une recharge et effacée deux secondes après la dernière. Rien en permanence à l'écran —
 * exigence du handoff. Écrit sans client : disposition fonctionnelle (tuiles dessinées), à vérifier
 * et retravailler à l'œil, à toutes les échelles d'interface.
 */
public class CooldownHud implements IGuiOverlay {
    private static final int LINGER_TICKS = 40; // deux secondes après la dernière recharge
    private static final int TILE = 20;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        PlayerSkills skills = PlayerSkillsProvider.get(player).orElse(null);
        if (skills == null) {
            return;
        }
        long now = mc.level.getGameTime();

        List<Tile> tiles = new ArrayList<>();
        for (var entry : skills.cooldowns().entrySet()) {
            long cooldown = cooldownTicksOf(entry.getKey());
            if (cooldown <= 0) {
                continue;
            }
            OptionalLong last = OptionalLong.of(entry.getValue());
            long remaining = Cooldowns.remaining(last, cooldown, now);
            long sinceReady = now - (entry.getValue() + cooldown);
            if (remaining > 0) {
                tiles.add(new Tile(1f - (float) remaining / cooldown, (int) Math.ceil(remaining / 20.0), false));
            } else if (sinceReady >= 0 && sinceReady < LINGER_TICKS) {
                tiles.add(new Tile(1f, 0, true)); // prêt, affiché encore ~2 s puis effacé
            }
        }
        if (tiles.isEmpty()) {
            return;
        }
        tiles = tiles.subList(0, Math.min(3, tiles.size()));

        int totalWidth = tiles.size() * (TILE + 2) - 2;
        int x = width / 2 - totalWidth / 2;
        int y = height - 22 - TILE - 4; // au-dessus de la barre d'objets
        for (Tile tile : tiles) {
            graphics.fill(x, y, x + TILE, y + TILE, 0x90000000);
            int fillHeight = (int) (TILE * tile.progress());
            int colour = tile.ready() ? 0xC055FF55 : 0xC0FFD040;
            graphics.fill(x, y + (TILE - fillHeight), x + TILE, y + TILE, colour);
            if (!tile.ready()) {
                graphics.drawCenteredString(mc.font, String.valueOf(tile.seconds()), x + TILE / 2, y + 6, 0xFFFFFFFF);
            }
            x += TILE + 2;
        }
    }

    private static long cooldownTicksOf(String nodeId) {
        for (SkillTree tree : ClientSkillTrees.all().values()) {
            for (Tier tier : tree.tiers()) {
                for (Node node : tier.nodes()) {
                    if (node.id().equals(nodeId)) {
                        return node.effects().stream()
                                .filter(effect -> effect.type() == EffectType.ABILITY)
                                .map(effect -> effect.fields())
                                .filter(fields -> fields.has("cooldown_ticks"))
                                .mapToLong(fields -> fields.get("cooldown_ticks").getAsLong())
                                .findFirst().orElse(0L);
                    }
                }
            }
        }
        return 0L;
    }

    private record Tile(float progress, int seconds, boolean ready) {
    }
}
