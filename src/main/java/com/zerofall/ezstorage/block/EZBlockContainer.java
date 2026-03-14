package com.zerofall.ezstorage.block;

import net.minecraft.Container;
import net.minecraft.ITileEntityProvider;
import net.minecraft.Material;
import net.minecraft.ServerPlayer;
import net.minecraft.TileEntity;
import net.minecraft.World;

import com.zerofall.ezstorage.EZTab;
import com.zerofall.ezstorage.EZStorage;
import com.zerofall.ezstorage.container.ContainerStorageCore;
import com.zerofall.ezstorage.container.ContainerStorageCoreCrafting;
import com.zerofall.ezstorage.init.EZBlocks;
import com.zerofall.ezstorage.network.S2C.S2COpenGuiPacket;
import com.zerofall.ezstorage.network.S2C.S2CStoragePacket;
import com.zerofall.ezstorage.tileentity.TileEntityStorageCore;
import com.zerofall.ezstorage.util.EZInventory;
import moddedmite.rustedironcore.network.Network;

public class EZBlockContainer extends StorageMultiblock implements ITileEntityProvider {

    protected EZBlockContainer(int id, String name, Material materialIn) {
        super(id, name, materialIn);
        this.setUnlocalizedName(name);
        this.setCreativeTab(EZTab.TAB);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn) {
        return null;
    }

    public void openPlayerInventoryGui(ServerPlayer player, EZInventory inventory, World worldIn, int x, int y, int z, TileEntityStorageCore core)
    {
        boolean enableCraftingGrid = core.hasCraftBox;
        EZStorage.instance.guiHandler.inventoryIds.put(player, inventory.id);
        Container container;
        int guiX = x;
        int guiY = y;
        int guiZ = z;

        if (enableCraftingGrid)
        {
            int craftX = core.craftBoxX;
            int craftY = core.craftBoxY;
            int craftZ = core.craftBoxZ;

            if (worldIn.getBlock(craftX, craftY, craftZ) != EZBlocks.crafting_box)
            {
                craftX = x;
                craftY = y;
                craftZ = z;
            }

            container = new ContainerStorageCoreCrafting(player, worldIn, inventory, craftX, craftY, craftZ);
            guiX = craftX;
            guiY = craftY;
            guiZ = craftZ;
        }
        else
        {
            container = new ContainerStorageCore(player, inventory);
        }

        if (player.openContainer != player.inventoryContainer)
        {
            player.closeScreen();
        }
        // Manually increment windowId (incrementWindowID is private in ServerPlayer)
        player.currentWindowId = (player.currentWindowId % 100) + 1;
        int guiId = enableCraftingGrid ? 2 : 1;

        player.openContainer = container;
        player.openContainer.windowId = player.currentWindowId;


        Network.sendToClient(player, new S2COpenGuiPacket(guiId, player.currentWindowId, guiX, guiY, guiZ));
        Network.sendToClient(player, new S2CStoragePacket(inventory));
    }
}