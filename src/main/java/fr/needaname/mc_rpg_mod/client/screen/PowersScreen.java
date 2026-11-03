package fr.needaname.mc_rpg_mod.client.screen;

import fr.needaname.mc_rpg_mod.network.AssignPowerPacket;
import fr.needaname.mc_rpg_mod.network.RpgNetwork;
import fr.needaname.mc_rpg_mod.skill.player.PlayerSkills;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Écran des pouvoirs : trois emplacements (R / G / X) remplis depuis la liste des pouvoirs débloqués,
 * tout modifiable. Fonctionnel ; l'auto-placement des trois premiers débloqués reste à ajouter
 * (côté serveur) et à régler en jeu.
 */
public class PowersScreen extends Screen {
    private static final String[] SLOTS = {"R", "G", "X"};

    public PowersScreen() {
        super(Component.literal("Pouvoirs"));
    }

    @Override
    protected void init() {
        PlayerSkills skills = SkillScreens.skills().orElse(new PlayerSkills());

        int y = 40;
        for (String slot : SLOTS) {
            addRenderableWidget(Button.builder(Component.literal("Vider " + slot), b -> {
                RpgNetwork.sendToServer(new AssignPowerPacket(slot, ""));
                this.minecraft.setScreen(new PowersScreen());
            }).bounds(this.width - 90, y, 80, 18).build());
            y += 22;
        }

        y = 110;
        List<SkillScreens.PowerRef> powers = SkillScreens.unlockedPowers(skills);
        for (SkillScreens.PowerRef power : powers) {
            int bx = this.width / 2 - 30;
            for (String slot : SLOTS) {
                addRenderableWidget(Button.builder(Component.literal(slot), b -> {
                    RpgNetwork.sendToServer(new AssignPowerPacket(slot, power.nodeId()));
                    this.minecraft.setScreen(new PowersScreen());
                }).bounds(bx, y, 20, 18).build());
                bx += 22;
            }
            y += 22;
        }

        addRenderableWidget(Button.builder(Component.literal("Retour"),
                        b -> this.minecraft.setScreen(new OverviewScreen()))
                .bounds(this.width / 2 - 50, this.height - 26, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        PlayerSkills skills = SkillScreens.skills().orElse(new PlayerSkills());
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);

        int y = 40;
        for (String slot : SLOTS) {
            String assigned = skills.powerSlot(slot).orElse("— vide —");
            graphics.drawString(this.font, "Emplacement " + slot + " : " + assigned, 16, y + 4, 0xFFFFFFFF);
            y += 22;
        }
        graphics.drawString(this.font, "Pouvoirs débloqués (assigner à R / G / X) :", 16, 96, 0xFFFFD040);
        int ly = 110;
        for (SkillScreens.PowerRef power : SkillScreens.unlockedPowers(skills)) {
            graphics.drawString(this.font, power.name() + " (" + power.treeId() + ")", 16, ly + 4, 0xFFFFFFFF);
            ly += 22;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
