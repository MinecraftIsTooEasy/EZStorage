package com.zerofall.ezstorage;

import com.zerofall.ezstorage.util.EZInventoryManager;
import moddedmite.rustedironcore.api.event.listener.ITickListener;
import net.minecraft.server.MinecraftServer;

/**
 * RustedIronCore-based event listener for tick-based world save/unload handling.
 */
public class EZRICEvents implements ITickListener {

    private int saveTickCounter = 0;
    private static final int SAVE_INTERVAL = 200; // 10 seconds=

    @Override
    public void onServerTick(MinecraftServer server) {
        saveTickCounter++;
        if (saveTickCounter >= SAVE_INTERVAL) {
            saveTickCounter = 0;
            EZInventoryManager.saveInventories();
        }
    }
}