package fr.needaname.mc_rpg_mod.skill.player;

import fr.needaname.mc_rpg_mod.RpgMod;
import fr.needaname.mc_rpg_mod.effect.Filters;
import fr.needaname.mc_rpg_mod.skill.SkillTreeManager;
import fr.needaname.mc_rpg_mod.skill.model.XpSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Attribue l'XP à partir des {@code xp_sources} des barèmes. Deux formes :
 * <ul>
 *   <li><b>statistique vanilla cumulative</b> ({@code stat}, {@code per}) : relevée à chaque tick
 *       serveur, convertie en XP par pas via une base mémorisée par joueur ;</li>
 *   <li><b>événement ponctuel</b> ({@code on}) : routé par {@link SkillXp#forEvent}.</li>
 * </ul>
 *
 * <p>Sont câblés ici les événements à correspondance directe et sûre : {@code break_block},
 * {@code kill} (avec {@code require.ranged}), {@code breed}, {@code tame}, {@code eat},
 * {@code shield_block}, {@code trade}, et les cumulatives {@code damage_survived} et {@code heal}.
 *
 * <p>Restent à câbler (détection sur mesure, à faire et vérifier en jeu — points d'ancrage prêts via
 * {@link SkillXp#forEvent}) : {@code harvest_mature} et {@code plant} (état de maturité / pose de bloc),
 * {@code shear} (pas d'événement Forge dédié en 1.20.1), {@code ranged_hit} (+{@code distance_over}),
 * {@code use_item} des engrais ({@code BonemealEvent}), {@code surface_from_depth} (+{@code depth_over}).
 * Tant qu'ils ne le sont pas, Flore et Combat à distance ne gagnent pas encore d'XP.
 *
 * <p>Tout est strictement côté serveur (ServerPlayer).
 */
@Mod.EventBusSubscriber(modid = RpgMod.MODID)
public final class XpSourceEvents {
    /** En-deçà de ce déplacement horizontal par tick, le joueur n'est pas compté « actif ». */
    private static final double ACTIVE_SPEED_SQR = 0.01 * 0.01;

    private XpSourceEvents() {
    }

    // --- Sources « événement » ---

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        SkillXp.forEvent(player, "break_block", (tree, src) ->
                src.filter().map(f -> Filters.matchesBlock(f, event.getState())).orElse(true)
                        && src.require().isEmpty());
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        boolean ranged = event.getSource().is(DamageTypeTags.IS_PROJECTILE);
        var victimType = event.getEntity().getType();
        SkillXp.forEvent(killer, "kill", (tree, src) -> {
            if (src.filter().isPresent() && !Filters.matchesEntity(src.filter().get(), victimType)) {
                return false;
            }
            return requireRangedSatisfied(src, ranged);
        });
    }

    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer player) {
            SkillXp.forEvent(player, "breed", (tree, src) -> src.require().isEmpty());
        }
    }

    @SubscribeEvent
    public static void onTame(AnimalTameEvent event) {
        if (event.getTamer() instanceof ServerPlayer player) {
            SkillXp.forEvent(player, "tame", (tree, src) -> src.require().isEmpty());
        }
    }

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getItem().getItem().isEdible()) {
            SkillXp.forEvent(player, "eat", (tree, src) -> src.require().isEmpty());
        }
    }

    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillXp.forEvent(player, "shield_block", (tree, src) -> src.require().isEmpty());
        }
    }

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillXp.forEvent(player, "trade", (tree, src) -> src.require().isEmpty());
        }
    }

    // --- Sources cumulatives : dégâts survécus et PV régénérés (Vitalité) ---

    /** Dégâts bruts du coup en cours, relevés avant toute réduction, en attendant de savoir s'il est survécu. */
    private static final Map<UUID, Float> RAW_DAMAGE = new HashMap<>();

    /**
     * HIGHEST : le montant est encore brut, avant l'armure, les enchantements de protection,
     * l'absorption et les propres réductions du mod ({@code damage_taken}). Un coup bloqué au bouclier
     * n'arrive pas jusqu'ici.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurtRaw(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RAW_DAMAGE.put(player.getUUID(), event.getAmount());
        }
    }

    /** Après l'armure : le coup est « survécu » si les dégâts finaux ne vident pas la vie (absorption comprise). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamageFinal(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Float raw = RAW_DAMAGE.remove(player.getUUID());
        if (raw != null && event.getAmount() < player.getHealth() + player.getAbsorptionAmount()) {
            SkillXp.accumulate(player, "damage_survived", raw);
        }
    }

    /** Seuls les PV réellement rendus comptent : se soigner à pleine vie ne rapporte rien. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            float healed = Math.min(event.getAmount(), player.getMaxHealth() - player.getHealth());
            SkillXp.accumulate(player, "heal", healed);
        }
    }

    @SubscribeEvent
    public static void onLogoutClearDamage(PlayerEvent.PlayerLoggedOutEvent event) {
        RAW_DAMAGE.remove(event.getEntity().getUUID());
    }

    // --- Sources « statistique » + suivi du temps actif (xp_on_death) ---

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            trackActiveTime(player, skills);
            awardStatSources(player, skills);
        });
    }

    /** « Actif » = le joueur s'est déplacé horizontalement ce tick (proxy simple, signalé en PR). */
    private static void trackActiveTime(ServerPlayer player, PlayerSkills skills) {
        Vec3 motion = player.getDeltaMovement();
        double horizontalSqr = motion.x * motion.x + motion.z * motion.z;
        if (horizontalSqr > ACTIVE_SPEED_SQR) {
            skills.addActiveTicks(1);
        }
    }

    private static void awardStatSources(ServerPlayer player, PlayerSkills skills) {
        for (var tree : SkillTreeManager.INSTANCE.trees().values()) {
            for (XpSource source : tree.xpSources()) {
                if (!(source instanceof XpSource.Stat stat)) {
                    continue;
                }
                ResourceLocation parsed = ResourceLocation.tryParse(stat.stat());
                // Le registre des stats custom indexe ses valeurs par identité : il faut l'instance
                // enregistrée, pas un ResourceLocation égal (sinon NPE dans Stat.buildName).
                ResourceLocation statId = parsed == null ? null : BuiltInRegistries.CUSTOM_STAT.get(parsed);
                if (statId == null || stat.per() <= 0) {
                    continue;
                }
                long current = player.getStats().getValue(Stats.CUSTOM.get(statId));
                if (!skills.hasStatBaseline(stat.stat())) {
                    // Pas de rétroactivité : ce que le joueur a fait avant le mod (ou avant l'ajout de
                    // cette source au barème) ne rapporte rien. Seul le jeu à venir compte.
                    skills.setStatBaseline(stat.stat(), current);
                    continue;
                }
                long baseline = skills.statBaseline(stat.stat());
                long steps = (current - baseline) / stat.per();
                if (steps > 0) {
                    SkillXp.grant(player, tree.id(), steps * stat.xp());
                    skills.setStatBaseline(stat.stat(), baseline + steps * (long) stat.per());
                }
            }
        }
    }

    private static boolean requireRangedSatisfied(XpSource.Event src, boolean ranged) {
        if (src.require().isEmpty()) {
            return true;
        }
        var require = src.require().get();
        // Seule la clé 'ranged' est interprétée ici ; toute autre condition non évaluée écarte la source.
        if (require.size() == 1 && require.has("ranged")) {
            return require.get("ranged").getAsBoolean() == ranged;
        }
        return false;
    }
}
