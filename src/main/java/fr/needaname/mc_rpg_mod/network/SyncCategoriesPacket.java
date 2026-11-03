package fr.needaname.mc_rpg_mod.network;

import fr.needaname.mc_rpg_mod.category.CategorySavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Serveur → client : les états de catégorie non actifs ({@code id → état}). Envoyé à la connexion et
 * à tous les joueurs après chaque changement, pour qu'un arbre masqué disparaisse sans reconnexion.
 */
public record SyncCategoriesPacket(CompoundTag data) {

    public static SyncCategoriesPacket of(CategorySavedData saved) {
        CompoundTag tag = new CompoundTag();
        saved.states().forEach((id, state) -> tag.putString(id, state.id()));
        return new SyncCategoriesPacket(tag);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public static SyncCategoriesPacket decode(FriendlyByteBuf buf) {
        CompoundTag data = buf.readNbt();
        return new SyncCategoriesPacket(data != null ? data : new CompoundTag());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleCategories(this));
    }
}
