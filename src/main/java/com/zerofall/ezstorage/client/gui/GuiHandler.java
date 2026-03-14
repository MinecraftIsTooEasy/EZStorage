package com.zerofall.ezstorage.client.gui;

import java.util.HashMap;

import net.minecraft.EntityPlayer;
import net.minecraft.World;

import com.zerofall.ezstorage.container.ContainerStorageCore;
import com.zerofall.ezstorage.container.ContainerStorageCoreCrafting;
import com.zerofall.ezstorage.util.EZInventory;
import com.zerofall.ezstorage.util.EZInventoryManager;

public class GuiHandler {

    public HashMap<EntityPlayer, String> inventoryIds = new HashMap<>();

    public Object getServerGuiElement(int ID, EntityPlayer player, World world)
    {
        if (inventoryIds.containsKey(player))
        {
            String inventoryId = inventoryIds.remove(player);
            EZInventory inventory = EZInventoryManager.getInventory(inventoryId);

            if (inventory != null)
            {
                if (ID == 1)
                {
                    return new ContainerStorageCore(player, inventory);
                }
                else if (ID == 2)
                {
                    return new ContainerStorageCoreCrafting(player, world, inventory);
                }
            }
        }

        return null;
    }

    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
    {
        if (ID == 1)
        {
            return new GuiStorageCore(player, world, x, y, z);
        } else if (ID == 2)
        {
            return new GuiCraftingCore(player, world, x, y, z);
        }

        return null;
    }

}