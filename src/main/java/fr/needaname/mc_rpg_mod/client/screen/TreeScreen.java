package fr.needaname.mc_rpg_mod.client.screen;

import fr.needaname.mc_rpg_mod.network.ChooseNodePacket;
import fr.needaname.mc_rpg_mod.network.RpgNetwork;
import fr.needaname.mc_rpg_mod.skill.ClientSkillTrees;
import fr.needaname.mc_rpg_mod.skill.model.Node;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import fr.needaname.mc_rpg_mod.skill.model.Tier;
import fr.needaname.mc_rpg_mod.skill.player.PlayerSkills;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Écran d'un arbre : les paliers, les choix exclusifs (un seul par palier), l'état de chaque nœud.
 * Les cinq états sont marqués par un symbole ET un libellé ET une couleur — jamais la couleur seule.
 * Fonctionnel, à retravailler à l'œil (panneau de détail, deux cases côte à côte) selon les maquettes.
 */
public class TreeScreen extends Screen {
    private record Line(String text, int y, int color) {
    }

    private final String treeId;
    private final List<Line> lines = new ArrayList<>();

    public TreeScreen(String treeId) {
        super(Component.literal("Arbre"));
        this.treeId = treeId;
    }

    @Override
    protected void init() {
        lines.clear();
        SkillTree tree = ClientSkillTrees.byId(treeId).orElse(null);
        PlayerSkills skills = SkillScreens.skills().orElse(new PlayerSkills());
        if (tree == null) {
            lines.add(new Line("Barème non reçu : " + treeId, 40, 0xFFFF7070));
            addBackButton();
            return;
        }
        int y = 40;
        int level = skills.level(tree.id());
        for (Tier tier : tree.tiers()) {
            lines.add(new Line("Palier " + tier.level() + (tier.exclusive() ? "  (un choix)" : "")
                    + "   —   niveau requis " + tier.level() + (level >= tier.level() ? " ✓" : ""),
                    y, 0xFFFFFFFF));
            y += 13;
            for (Node node : tier.nodes()) {
                SkillScreens.NodeDisplay state = SkillScreens.stateOf(tree, tier, node, skills);
                lines.add(new Line("  " + state.marker + " " + node.name() + "  [" + state.label + "]",
                        y, state.color));
                lines.add(new Line("      " + node.description(), y + 10, 0xFFAAAAAA));
                if (state == SkillScreens.NodeDisplay.TO_PLACE) {
                    String nodeId = node.id();
                    addRenderableWidget(Button.builder(Component.literal("Choisir"), b -> {
                        RpgNetwork.sendToServer(new ChooseNodePacket(nodeId));
                        this.minecraft.setScreen(new TreeScreen(treeId)); // se rafraîchit à la synchro suivante
                    }).bounds(this.width - 90, y, 80, 18).build());
                }
                y += 26;
            }
            y += 6;
        }
        addBackButton();
    }

    private void addBackButton() {
        addRenderableWidget(Button.builder(Component.literal("Retour"),
                        b -> this.minecraft.setScreen(new OverviewScreen()))
                .bounds(this.width / 2 - 50, this.height - 26, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        SkillTree tree = ClientSkillTrees.byId(treeId).orElse(null);
        graphics.drawCenteredString(this.font, tree != null ? tree.name() : treeId, this.width / 2, 16, 0xFFFFFFFF);
        for (Line line : lines) {
            graphics.drawString(this.font, line.text(), 16, line.y(), line.color());
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
