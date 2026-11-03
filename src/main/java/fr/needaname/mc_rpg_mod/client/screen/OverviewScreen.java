package fr.needaname.mc_rpg_mod.client.screen;

import fr.needaname.mc_rpg_mod.skill.ClientSkillTrees;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import fr.needaname.mc_rpg_mod.skill.player.PlayerSkills;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Vue d'ensemble : les dix compétences, niveau et XP, pastille « à placer », un arbre masqué
 * n'apparaît pas, un arbre désactivé est grisé avec la mention. Écrit sans client : mise en page
 * fonctionnelle (listes + boutons), à retravailler à l'œil selon les maquettes en session locale.
 */
public class OverviewScreen extends Screen {
    public OverviewScreen() {
        super(Component.translatable("screen.mc_rpg_mod.overview"));
    }

    @Override
    protected void init() {
        PlayerSkills skills = SkillScreens.skills().orElse(new PlayerSkills());
        List<SkillTree> trees = ClientSkillTrees.all().values().stream()
                .filter(tree -> SkillScreens.visible(tree.id()))
                .sorted(Comparator.comparing(SkillTree::id))
                .toList();

        int rowWidth = 220;
        int x = this.width / 2 - rowWidth / 2;
        int y = 40;
        for (SkillTree tree : trees) {
            int level = skills.level(tree.id());
            boolean disabled = SkillScreens.disabled(tree.id());
            boolean pending = SkillScreens.hasPendingChoice(tree, skills);
            StringBuilder label = new StringBuilder(tree.name()).append(" — niv. ").append(level);
            if (pending) {
                label.append("  ◆ à placer");
            }
            if (disabled) {
                label.append("  (désactivé)");
            }
            Button button = Button.builder(Component.literal(label.toString()),
                            b -> this.minecraft.setScreen(new TreeScreen(tree.id())))
                    .bounds(x, y, rowWidth, 20).build();
            button.active = !disabled;
            addRenderableWidget(button);
            y += 24;
        }

        addRenderableWidget(Button.builder(Component.literal("Pouvoirs"),
                        b -> this.minecraft.setScreen(new PowersScreen()))
                .bounds(this.width / 2 - 100, y + 6, 95, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Fermer"), b -> onClose())
                .bounds(this.width / 2 + 5, y + 6, 95, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 18, 0xFFFFFFFF);
        if (ClientSkillTrees.all().isEmpty()) {
            graphics.drawCenteredString(this.font, Component.literal("Barèmes non reçus du serveur…"),
                    this.width / 2, this.height / 2, 0xFFFF7070);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
