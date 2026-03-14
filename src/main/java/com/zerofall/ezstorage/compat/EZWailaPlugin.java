package com.zerofall.ezstorage.compat;

import com.zerofall.ezstorage.block.BlockCraftingBox;
import com.zerofall.ezstorage.block.BlockStorage;
import com.zerofall.ezstorage.block.StorageMultiblock;
import com.zerofall.ezstorage.configuration.EZConfiguration;
import com.zerofall.ezstorage.tileentity.TileEntityInventoryProxy;
import com.zerofall.ezstorage.tileentity.TileEntityStorageCore;
import com.zerofall.ezstorage.util.BlockRef;
import com.zerofall.ezstorage.util.EZInventory;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import net.minecraft.Block;
import net.minecraft.ItemStack;
import net.minecraft.NBTTagCompound;
import net.minecraft.RaycastCollision;
import net.minecraft.ServerPlayer;
import net.minecraft.StatCollector;
import net.minecraft.TileEntity;
import net.minecraft.World;

import java.text.DecimalFormat;
import java.util.List;

/** Waila integration provider for EZStorage multiblock blocks. */
public class EZWailaPlugin implements IWailaDataProvider {

    private static final String NBT_HAS_CORE = "EZStorageHasCore";
    private static final String NBT_CORE_X = "EZStorageCoreX";
    private static final String NBT_CORE_Y = "EZStorageCoreY";
    private static final String NBT_CORE_Z = "EZStorageCoreZ";
    private static final String NBT_ITEMS_STORED = "EZStorageItemsStored";
    private static final String NBT_ITEMS_MAX = "EZStorageItemsMax";
    private static final String NBT_TYPES_STORED = "EZStorageTypesStored";
    private static final String NBT_TYPES_MAX = "EZStorageTypesMax";

    private static final EZWailaPlugin INSTANCE = new EZWailaPlugin();
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,###");

    public static void register(IWailaRegistrar registrar) {
        registrar.registerBodyProvider(INSTANCE, StorageMultiblock.class);
        registrar.registerNBTProvider(INSTANCE, TileEntityStorageCore.class);
        registrar.registerNBTProvider(INSTANCE, TileEntityInventoryProxy.class);
    }

    @Override
    public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return null;
    }

    @Override
    public List<String> getWailaHead(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return currenttip;
    }

    @Override
    public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        Block block = accessor.getBlock();

        if (block instanceof BlockStorage storage)
        {
            currenttip.add(StatCollector.translateToLocalFormatted("hud.msg.ezstorage.storage.capacity", format(storage.getCapacity())));
        }

        if (block instanceof BlockCraftingBox)
        {
            String tierName = BlockCraftingBox.getTierName(accessor.getMetadata());
            currenttip.add(StatCollector.translateToLocalFormatted("ezstorage.crafting_box.tier_label", tierName));
        }

        NBTTagCompound tag = accessor.getNBTData();

        if (tag != null && tag.getBoolean(NBT_HAS_CORE))
        {
            appendCoreInfo(currenttip, tag.getInteger(NBT_CORE_X), tag.getInteger(NBT_CORE_Y), tag.getInteger(NBT_CORE_Z));
            appendInventoryInfo(currenttip, tag.getLong(NBT_ITEMS_STORED), tag.getLong(NBT_ITEMS_MAX), tag.getInteger(NBT_TYPES_STORED), tag.getInteger(NBT_TYPES_MAX));
            return currenttip;
        }

        TileEntityStorageCore core = findCore(accessor);

        if (core == null)
        {
            currenttip.add(StatCollector.translateToLocal("hud.msg.ezstorage.waila.no_core"));
            return currenttip;
        }

        appendCoreInfo(currenttip, core.xCoord, core.yCoord, core.zCoord);

        EZInventory inventory = core.getInventory();
        if (inventory != null)
        {
            int maxTypes = EZConfiguration.maxItemTypes.getIntegerValue();
            appendInventoryInfo(currenttip, inventory.getTotalCount(), inventory.maxItems, inventory.slotCount(), maxTypes);
        }

        return currenttip;
    }

    @Override
    public List<String> getWailaTail(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return currenttip;
    }

    @Override
    public NBTTagCompound getNBTData(ServerPlayer player, TileEntity te, NBTTagCompound tag, World world, int x, int y, int z)
    {
        TileEntityStorageCore core = null;

        if (te instanceof TileEntityStorageCore storageCore)
        {
            core = storageCore;
        }

        else if (te instanceof TileEntityInventoryProxy proxy)
        {
            core = proxy.core;
        }

        if (core == null)
        {
            return tag;
        }

        tag.setBoolean(NBT_HAS_CORE, true);
        tag.setInteger(NBT_CORE_X, core.xCoord);
        tag.setInteger(NBT_CORE_Y, core.yCoord);
        tag.setInteger(NBT_CORE_Z, core.zCoord);

        EZInventory inventory = core.getInventory();

        if (inventory != null)
        {
            tag.setLong(NBT_ITEMS_STORED, inventory.getTotalCount());
            tag.setLong(NBT_ITEMS_MAX, inventory.maxItems);
            tag.setInteger(NBT_TYPES_STORED, inventory.slotCount());
            tag.setInteger(NBT_TYPES_MAX, EZConfiguration.maxItemTypes.getIntegerValue());
        }

        return tag;
    }

    private static TileEntityStorageCore findCore(IWailaDataAccessor accessor)
    {
        TileEntity tileEntity = accessor.getTileEntity();

        if (tileEntity instanceof TileEntityStorageCore storageCore)
        {
            return storageCore;
        }

        if (tileEntity instanceof TileEntityInventoryProxy proxy && proxy.core != null)
        {
            return proxy.core;
        }

        Block block = accessor.getBlock();

        if (!(block instanceof StorageMultiblock multiblock))
        {
            return null;
        }

        RaycastCollision hit = accessor.getPosition();
        if (hit == null)
        {
            return null;
        }

        return multiblock.findCore(new BlockRef(block, hit.block_hit_x, hit.block_hit_y, hit.block_hit_z), accessor.getWorld(), null);
    }

    private static void appendCoreInfo(List<String> tooltip, int x, int y, int z) {
        tooltip.add(StatCollector.translateToLocalFormatted("hud.msg.ezstorage.waila.connected", x, y, z));
    }

    private static void appendInventoryInfo(List<String> tooltip, long itemsStored, long itemsMax, int typesStored, int typesMax) {
        tooltip.add(StatCollector.translateToLocalFormatted("hud.msg.ezstorage.core.itemscount", format(itemsStored), format(itemsMax)));

        String maxTypesText = typesMax <= 0 ? "INF" : format(typesMax);
        tooltip.add(StatCollector.translateToLocalFormatted("hud.msg.ezstorage.core.typescount", format(typesStored), maxTypesText));
    }

    private static String format(long value) {
        return NUMBER_FORMAT.format(value);
    }
}