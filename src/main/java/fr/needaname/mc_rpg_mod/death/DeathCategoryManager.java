package fr.needaname.mc_rpg_mod.death;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import fr.needaname.mc_rpg_mod.RpgMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Charge {@code data/<namespace>/rpgmod/death_categories/*.json}, rechargé par /reload.
 * L'id d'une catégorie est celui du fichier (ex. {@code rpgmod:fire}).
 * Une mort est rangée dans la première catégorie qui matche, par priorité décroissante ;
 * si aucune ne matche, elle compte dans {@link #FALLBACK}.
 */
public class DeathCategoryManager extends SimpleJsonResourceReloadListener {
    // GSON doit être initialisé avant INSTANCE (ordre d'initialisation statique).
    private static final Gson GSON = new GsonBuilder().create();
    public static final DeathCategoryManager INSTANCE = new DeathCategoryManager();
    public static final ResourceLocation FALLBACK = ResourceLocation.fromNamespaceAndPath(RpgMod.MODID, "other");

    private volatile List<Entry> ordered = List.of();

    public record Entry(ResourceLocation id, DeathCategory category) {}

    private DeathCategoryManager() {
        super(GSON, RpgMod.MODID + "/death_categories");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<Entry> loaded = new ArrayList<>();
        jsons.forEach((id, json) -> DeathCategory.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> RpgMod.LOGGER.error("Invalid death category {}: {}", id, error))
                .ifPresent(category -> {
                    if (category.isEmpty()) {
                        RpgMod.LOGGER.warn("Death category {} has no criteria, ignored", id);
                    } else {
                        loaded.add(new Entry(id, category));
                    }
                }));
        loaded.sort(Comparator.comparingInt((Entry e) -> e.category().priority()).reversed()
                .thenComparing(e -> e.id().toString()));
        ordered = List.copyOf(loaded);
        RpgMod.LOGGER.info("Loaded {} death categories", ordered.size());
    }

    public ResourceLocation classify(DamageSource source) {
        for (Entry entry : ordered) {
            if (entry.category().matches(source)) {
                return entry.id();
            }
        }
        return FALLBACK;
    }

    public List<Entry> categories() {
        return ordered;
    }
}
