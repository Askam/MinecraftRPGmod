package fr.needaname.mc_rpg_mod.death;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Compteurs de morts d'un joueur : un total global + un compteur par cause.
 *
 * <p>Depuis le bloc 3, la cause est l'id d'une entrée {@code counters.per_cause} de {@code mort.json}
 * (ex. {@code chute}, {@code vide}, {@code creeper}), source de vérité du design — et non plus l'id
 * d'un ancien {@code death_categories}. Les compteurs d'une cause retirée du barème sont conservés,
 * pour ne rien perdre si elle revient.
 *
 * <p>La capability, sa recopie dans {@code Clone} et la synchro réseau restent inchangées : seul le
 * format de clé des compteurs change (aucune sauvegarde réelle à migrer).
 */
public class DeathStats {
    public static final Capability<DeathStats> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private final Map<String, Integer> byCause = new HashMap<>();
    private int total;

    public static Optional<DeathStats> get(Player player) {
        return player.getCapability(CAPABILITY).resolve();
    }

    public void recordDeath(String cause) {
        total++;
        byCause.merge(cause, 1, Integer::sum);
    }

    public int getTotal() {
        return total;
    }

    public int getCount(String cause) {
        return byCause.getOrDefault(cause, 0);
    }

    public Map<String, Integer> getAll() {
        return Collections.unmodifiableMap(byCause);
    }

    public void copyFrom(DeathStats other) {
        byCause.clear();
        byCause.putAll(other.byCause);
        total = other.total;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("total", total);
        CompoundTag causes = new CompoundTag();
        byCause.forEach(causes::putInt);
        tag.put("causes", causes);
        return tag;
    }

    public void load(CompoundTag tag) {
        byCause.clear();
        total = tag.getInt("total");
        CompoundTag causes = tag.getCompound("causes");
        for (String key : causes.getAllKeys()) {
            byCause.put(key, causes.getInt(key));
        }
    }
}
