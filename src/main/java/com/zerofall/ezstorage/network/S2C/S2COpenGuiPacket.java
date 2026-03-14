package com.zerofall.ezstorage.network.S2C;

import com.zerofall.ezstorage.EZStorage;
import com.zerofall.ezstorage.container.ContainerStorageCore;
import com.zerofall.ezstorage.container.ContainerStorageCoreCrafting;
import com.zerofall.ezstorage.client.gui.GuiCraftingCore;
import com.zerofall.ezstorage.client.gui.GuiStorageCore;
import moddedmite.rustedironcore.network.Packet;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.EntityPlayer;
import net.minecraft.Minecraft;
import net.minecraft.ResourceLocation;
import net.minecraft.World;

/**
 * S2C: Server tells client to open the storage GUI.
 * guiId: 1 = StorageCore, 2 = CraftingCore
 * x, y, z: block coordinates (passed through to GUI constructor).
 * windowId: the server-assigned window ID.
 */
public class S2COpenGuiPacket implements Packet {

    public static final ResourceLocation CHANNEL = new ResourceLocation(EZStorage.MOD_ID, "s2c_open_gui");

    private final int guiId;
    private final int windowId;
    private final int x;
    private final int y;
    private final int z;

    public S2COpenGuiPacket(int guiId, int windowId, int x, int y, int z) {
        this.guiId = guiId;
        this.windowId = windowId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public S2COpenGuiPacket(PacketByteBuf buf) {
        this.guiId = buf.readVarInt();
        this.windowId = buf.readVarInt();
        this.x = buf.readVarInt();
        this.y = buf.readVarInt();
        this.z = buf.readVarInt();
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(guiId);
        buf.writeVarInt(windowId);
        buf.writeVarInt(x);
        buf.writeVarInt(y);
        buf.writeVarInt(z);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void apply(EntityPlayer player)
    {
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;

        if (world == null) return;

        if (guiId == 1)
        {
            ContainerStorageCore container = new ContainerStorageCore(player);
            container.windowId = windowId;
            player.openContainer = container;
            mc.displayGuiScreen(new GuiStorageCore(container, world, x, y, z));
        }
        else if (guiId == 2)
        {
            ContainerStorageCoreCrafting container = new ContainerStorageCoreCrafting(player, world, x, y, z);
            container.windowId = windowId;
            player.openContainer = container;
            mc.displayGuiScreen(new GuiCraftingCore(container, world, x, y, z));
        }
    }

    @Override
    public ResourceLocation getChannel() {
        return CHANNEL;
    }
}