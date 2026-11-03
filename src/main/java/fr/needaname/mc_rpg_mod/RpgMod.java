package fr.needaname.mc_rpg_mod;

import com.mojang.logging.LogUtils;
import fr.needaname.mc_rpg_mod.command.RpgCommands;
import fr.needaname.mc_rpg_mod.death.DeathCategoryManager;
import fr.needaname.mc_rpg_mod.death.DeathStats;
import fr.needaname.mc_rpg_mod.network.RpgNetwork;
import fr.needaname.mc_rpg_mod.skill.SkillTreeManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(RpgMod.MODID)
public class RpgMod {
    // Modid provisoire : à changer ici ET dans gradle.properties (mod_id) le jour où le nom est fixé.
    public static final String MODID = "mc_rpg_mod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RpgMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        modBus.addListener(this::registerCapabilities);
        RpgNetwork.register();

        MinecraftForge.EVENT_BUS.addListener(this::addReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(DeathStats.class);
    }

    private void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(SkillTreeManager.INSTANCE);
        event.addListener(DeathCategoryManager.INSTANCE);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        RpgCommands.register(event.getDispatcher());
    }
}
