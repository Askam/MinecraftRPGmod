package fr.needaname.mc_rpg_mod.skill.player;

import fr.needaname.mc_rpg_mod.RpgMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Attache un {@link PlayerSkills} à chaque joueur. Le jeton de capability vit ici, pas dans la
 * classe de données, pour que celle-ci reste testable sans le système de capabilities de Forge.
 *
 * <p>Comme pour {@code DeathStatsProvider}, on n'enregistre PAS {@code event.addListener(provider::invalidate)}
 * à l'attache : l'invalidation du {@link LazyOptional} est définitive et {@code reviveCaps()} ne la
 * défait pas (bug réel déjà rencontré sur les compteurs de mort). La recopie se fait dans
 * {@link SkillCapabilityEvents#onClone}.
 */
public class PlayerSkillsProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<PlayerSkills> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RpgMod.MODID, "player_skills");

    private final PlayerSkills skills = new PlayerSkills();
    private final LazyOptional<PlayerSkills> optional = LazyOptional.of(() -> skills);

    public static Optional<PlayerSkills> get(Player player) {
        return player.getCapability(CAPABILITY).resolve();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return CAPABILITY.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return skills.save();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        skills.load(tag);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
