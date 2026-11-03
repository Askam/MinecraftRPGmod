package fr.needaname.mc_rpg_mod.death;

import fr.needaname.mc_rpg_mod.RpgMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DeathStatsProvider implements ICapabilitySerializable<CompoundTag> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RpgMod.MODID, "death_stats");

    private final DeathStats stats = new DeathStats();
    private final LazyOptional<DeathStats> optional = LazyOptional.of(() -> stats);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return DeathStats.CAPABILITY.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return stats.save();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        stats.load(tag);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
