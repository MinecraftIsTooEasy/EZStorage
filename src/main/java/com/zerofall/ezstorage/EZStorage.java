package com.zerofall.ezstorage;

import com.zerofall.ezstorage.configuration.EZConfiguration;
import com.zerofall.ezstorage.network.EZStoragePacketHandler;
import fi.dy.masa.malilib.config.ConfigManager;
import moddedmite.rustedironcore.api.event.Handlers;
import net.fabricmc.api.ModInitializer;
import net.xiaoyu233.fml.FishModLoader;
import net.xiaoyu233.fml.ModResourceManager;
import net.xiaoyu233.fml.reload.event.MITEEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.zerofall.ezstorage.client.gui.GuiHandler;
import com.zerofall.ezstorage.compat.EZWailaPlugin;
import mcp.mobius.waila.api.impl.ModuleRegistrar;

public class EZStorage implements ModInitializer {

    public static final String MOD_ID = Reference.MOD_ID;

    public static EZStorage instance;

    public final Logger LOG = LogManager.getLogger(Reference.MOD_ID);
    public final GuiHandler guiHandler = new GuiHandler();

    public void onInitialize() {
        instance = this;
        ModResourceManager.addResourcePackDomain(Reference.MOD_ID);

        EZConfiguration.init();
        ConfigManager.getInstance().registerConfig(EZConfiguration.getInstance());

        MITEEvents.MITE_EVENT_BUS.register(new EZFMLEvents());
        Handlers.Tick.register(new EZRICEvents());

        EZStoragePacketHandler.registerAllPackets();

        if (FishModLoader.hasMod("waila")) {
            EZWailaPlugin.register(ModuleRegistrar.instance());
        }
    }
}