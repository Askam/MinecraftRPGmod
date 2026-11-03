package fr.needaname.mc_rpg_mod.network;

import fr.needaname.mc_rpg_mod.skill.SkillTreeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Serveur → client : le JSON source de chaque arbre chargé. Le client le relit avec le même
 * {@link fr.needaname.mc_rpg_mod.skill.SkillTreeParser} (un seul format, aucune divergence possible)
 * et le range dans {@link fr.needaname.mc_rpg_mod.skill.ClientSkillTrees}. Sans cet envoi, un serveur
 * dédié laisserait les écrans du client vides.
 */
public record SyncSkillTreesPacket(Map<String, String> raw) {
    private static final int MAX_ID = 256;
    private static final int MAX_JSON = 1 << 20; // 1 Mio par arbre, large de beaucoup

    public static SyncSkillTreesPacket of(SkillTreeManager manager) {
        return new SyncSkillTreesPacket(manager.rawByTreeId());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(raw.size());
        raw.forEach((id, json) -> {
            buf.writeUtf(id, MAX_ID);
            buf.writeUtf(json, MAX_JSON);
        });
    }

    public static SyncSkillTreesPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Map<String, String> raw = new HashMap<>();
        for (int i = 0; i < count; i++) {
            String id = buf.readUtf(MAX_ID);
            raw.put(id, buf.readUtf(MAX_JSON));
        }
        return new SyncSkillTreesPacket(raw);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSkillTrees(this));
    }
}
