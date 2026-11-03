package fr.needaname.mc_rpg_mod.client.screen;

import fr.needaname.mc_rpg_mod.category.CategoryState;
import fr.needaname.mc_rpg_mod.category.ClientCategoryState;
import fr.needaname.mc_rpg_mod.network.RpgNetwork;
import fr.needaname.mc_rpg_mod.network.SetCategoryPacket;
import fr.needaname.mc_rpg_mod.skill.ClientSkillTrees;
import fr.needaname.mc_rpg_mod.skill.model.SkillTree;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Écran d'administration (ouvert par le serveur via {@code /competence admin}). Trois états par
 * catégorie ; chaque changement part au serveur ({@link SetCategoryPacket}), qui revérifie la
 * permission. Les états lus sont ceux du client (mis à jour par la synchro), les comptes viennent de
 * l'instantané reçu à l'ouverture.
 */
public class AdminScreen extends Screen {
    private final CompoundTag counts;

    public AdminScreen(CompoundTag payload) {
        super(Component.literal("Administration des compétences"));
        this.counts = payload.getCompound("counts");
    }

    @Override
    protected void init() {
        List<SkillTree> trees = ClientSkillTrees.all().values().stream()
                .sorted(Comparator.comparing(SkillTree::id))
                .toList();
        int y = 40;
        for (SkillTree tree : trees) {
            int bx = this.width - 290;
            for (CategoryState state : CategoryState.values()) {
                addRenderableWidget(Button.builder(Component.literal(state.id()), b -> {
                    RpgNetwork.sendToServer(new SetCategoryPacket(tree.id(), state.id()));
                    this.minecraft.setScreen(new AdminScreen(wrap()));
                }).bounds(bx, y, 92, 18).build());
                bx += 96;
            }
            y += 24;
        }
        addRenderableWidget(Button.builder(Component.literal("Fermer"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 26, 100, 20).build());
    }

    private CompoundTag wrap() {
        CompoundTag tag = new CompoundTag();
        tag.put("counts", counts);
        return tag;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);
        List<SkillTree> trees = ClientSkillTrees.all().values().stream()
                .sorted(Comparator.comparing(SkillTree::id))
                .toList();
        int y = 40;
        for (SkillTree tree : trees) {
            CategoryState state = ClientCategoryState.state(tree.id());
            CompoundTag entry = counts.getCompound(tree.id());
            String info = entry.contains("players")
                    ? entry.getInt("players") + " j. · max " + entry.getInt("max")
                    : "aucun joueur";
            graphics.drawString(this.font, tree.name() + " — " + state.id() + "  (" + info + ")", 16, y + 5, 0xFFFFFFFF);
            y += 24;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
