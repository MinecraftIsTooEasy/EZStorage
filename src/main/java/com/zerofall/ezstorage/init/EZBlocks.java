package com.zerofall.ezstorage.init;

import net.minecraft.*;

import com.zerofall.ezstorage.Reference;
import com.zerofall.ezstorage.compat.EZCraftingBoxCompat;
import com.zerofall.ezstorage.block.BlockCondensedStorage;
import com.zerofall.ezstorage.block.BlockCraftingBox;
import com.zerofall.ezstorage.block.BlockHyperStorage;
import com.zerofall.ezstorage.block.BlockInputPort;
import com.zerofall.ezstorage.block.BlockStorage;
import com.zerofall.ezstorage.block.BlockStorageCable;
import com.zerofall.ezstorage.block.BlockStorageCore;
import com.zerofall.ezstorage.block.BlockStoragePanel;
import com.zerofall.ezstorage.item.ItemBlockCraftingBox;
import com.zerofall.ezstorage.item.ItemBlockStorage;
import com.zerofall.ezstorage.tileentity.TileEntityInventoryProxy;
import com.zerofall.ezstorage.tileentity.TileEntityStorageCore;

import net.xiaoyu233.fml.reload.event.BlockRegistryEvent;
import net.xiaoyu233.fml.reload.event.RecipeRegistryEvent;
import net.xiaoyu233.fml.reload.utils.IdUtil;

public class EZBlocks {

    public static Block storage_core;
    public static Block storage_box;
    public static Block condensed_storage_box;
    public static Block hyper_storage_box;
    public static Block input_port;
    public static Block crafting_box;
    public static Block storage_panel;
    public static Block storage_cable;

    public static void registerBlocks(BlockRegistryEvent event) {
        EZCraftingBoxCompat.initialize();

        int id;

        id = IdUtil.getNextBlockID();
        storage_core = new BlockStorageCore(id);
        TileEntity.addMapping(TileEntityStorageCore.class, "TileEntityStorageCore");
        Item.itemsList[id] = new ItemBlock(storage_core);
        event.registerBlock(Reference.MOD_NAME, Reference.MOD_ID + ":storage_core", "storage_core", storage_core);

        id = IdUtil.getNextBlockID();
        storage_box = new BlockStorage(id);
        Item.itemsList[id] = new ItemBlockStorage(storage_box);
        event.registerBlock(Reference.MOD_NAME, Reference.MOD_ID + ":storage_box", "storage_box", storage_box);

        id = IdUtil.getNextBlockID();
        condensed_storage_box = new BlockCondensedStorage(id);
        Item.itemsList[id] = new ItemBlockStorage(condensed_storage_box);
        event.registerBlock(Reference.MOD_NAME, Reference.MOD_ID + ":condensed_storage_box", "condensed_storage_box", condensed_storage_box);

        id = IdUtil.getNextBlockID();
        hyper_storage_box = new BlockHyperStorage(id);
        Item.itemsList[id] = new ItemBlockStorage(hyper_storage_box);
        event.registerBlock(Reference.MOD_NAME, Reference.MOD_ID + ":hyper_storage_box", "hyper_storage_box", hyper_storage_box);

        id = IdUtil.getNextBlockID();
        input_port = new BlockInputPort(id);
        TileEntity.addMapping(TileEntityInventoryProxy.class, "TileEntityInputPort");
        Item.itemsList[id] = new ItemBlock(input_port);
        event.registerBlock(Reference.MOD_NAME, Reference.MOD_ID + ":input_port", "input_port", input_port);

        id = IdUtil.getNextBlockID();
        crafting_box = new BlockCraftingBox(id);
        Item.itemsList[id] = new ItemBlockCraftingBox(crafting_box);
        event.registerBlock(Reference.MOD_NAME, Reference.MOD_ID + ":crafting_box", "crafting_box", crafting_box);

        id = IdUtil.getNextBlockID();
        storage_panel = new BlockStoragePanel(id);
        Item.itemsList[id] = new ItemBlock(storage_panel);
        event.registerBlock(Reference.MOD_NAME, Reference.MOD_ID + ":storage_panel", "storage_panel", storage_panel);

        id = IdUtil.getNextBlockID();
        storage_cable = new BlockStorageCable(id);
        Item.itemsList[id] = new ItemBlock(storage_cable);
        event.registerBlock(Reference.MOD_NAME, Reference.MOD_ID + ":storage_cable", "storage_cable", storage_cable);
    }

    public static void registerRecipes(RecipeRegistryEvent event) {
        EZCraftingBoxCompat.initialize();

        event.registerShapedRecipe(new ItemStack(storage_core), false, "BCB", "ABA", 'A', new ItemStack(Block.wood), 'B', new ItemStack(Item.stick), 'C', new ItemStack(Block.chest));
        event.registerShapedRecipe(new ItemStack(storage_box), false, "ABA", "BCB", "ABA", 'A', new ItemStack(Block.wood), 'B', new ItemStack(Block.planks), 'C', new ItemStack(Block.chest));
        event.registerShapedRecipe(new ItemStack(condensed_storage_box), false, "ACA", "EBE", "DCD", 'A', new ItemStack(Item.ingotIron), 'B', new ItemStack(storage_box), 'C', new ItemStack(Item.ingotGold), 'D', new ItemStack(Item.ingotIron), 'E', new ItemStack(Block.chest));
        event.registerShapedRecipe(new ItemStack(hyper_storage_box), false, "ABA", "ACA", "AAA", 'A', new ItemStack(Block.obsidian), 'B', new ItemStack(Item.netherStar), 'C', new ItemStack(condensed_storage_box));
        event.registerShapedRecipe(new ItemStack(input_port), false, " A ", " B ", " C ", 'A', new ItemStack(Block.hopperBlock), 'B', new ItemStack(Block.pistonBase), 'C', new ItemStack(Block.blockNetherQuartz));

        event.registerShapedRecipe(new ItemStack(crafting_box, 1, 0), false, " A ", " B ", " C ", 'A', new ItemStack(Item.enderPearl), 'B', new ItemStack(Block.workbench,1 ,0), 'C', new ItemStack(Item.flint));
        event.registerShapedRecipe(new ItemStack(crafting_box, 1, 0), false, " A ", " B ", " C ", 'A', new ItemStack(Item.enderPearl), 'B', new ItemStack(Block.workbench,1 ,1), 'C', new ItemStack(Item.flint));
        event.registerShapedRecipe(new ItemStack(crafting_box, 1, 0), false, " A ", " B ", " C ", 'A', new ItemStack(Item.enderPearl), 'B', new ItemStack(Block.workbench,1 ,2), 'C', new ItemStack(Item.flint));
        event.registerShapedRecipe(new ItemStack(crafting_box, 1, 0), false, " A ", " B ", " C ", 'A', new ItemStack(Item.enderPearl), 'B', new ItemStack(Block.workbench,1, 3), 'C', new ItemStack(Item.flint));
        event.registerShapedRecipe(new ItemStack(crafting_box, 1, 0), false, " A ", " B ", " C ", 'A', new ItemStack(Item.enderPearl), 'B', new ItemStack(Block.workbench,1 ,11), 'C', new ItemStack(Block.obsidian));
        event.registerShapedRecipe(new ItemStack(crafting_box, 1, 0), false, " A ", " B ", " C ", 'A', new ItemStack(Item.enderPearl), 'B', new ItemStack(Block.workbench,1 ,12), 'C', new ItemStack(Block.obsidian));
        event.registerShapedRecipe(new ItemStack(crafting_box, 1, 0), false, " A ", " B ", " C ", 'A', new ItemStack(Item.enderPearl), 'B', new ItemStack(Block.workbench,1 ,13), 'C', new ItemStack(Block.obsidian));
        event.registerShapedRecipe(new ItemStack(crafting_box, 1, 0), false, " A ", " B ", " C ", 'A', new ItemStack(Item.enderPearl), 'B', new ItemStack(Block.workbench,1 ,14), 'C', new ItemStack(Block.obsidian));

        event.registerShapedRecipe(new ItemStack(crafting_box, 1, 1), false, " A ", " B ", " C ", 'A', new ItemStack(Item.enderPearl), 'B', new ItemStack(Block.workbench, 1, 4), 'C', new ItemStack(Item.ingotCopper));
        event.registerShapedRecipe(new ItemStack(crafting_box, 1, 2), false, " A ", " B ", " C ", 'A', new ItemStack(Item.enderPearl), 'B', new ItemStack(Block.workbench, 1, 5), 'C', new ItemStack(Item.ingotSilver));
        event.registerShapedRecipe(new ItemStack(crafting_box, 1, 3), false, " A ", " B ", " C ", 'A', new ItemStack(Item.enderPearl), 'B', new ItemStack(Block.workbench, 1, 6), 'C', new ItemStack(Item.ingotGold));
        event.registerShapedRecipe(new ItemStack(crafting_box, 1, 4), false, " A ", " B ", " C ", 'A', new ItemStack(Item.enderPearl), 'B', new ItemStack(Block.workbench, 1, 7), 'C', new ItemStack(Item.ingotIron));
        event.registerShapedRecipe(new ItemStack(crafting_box, 1, 5), false, " A ", " B ", " C ", 'A', new ItemStack(Item.enderPearl), 'B', new ItemStack(Block.workbench, 1, 8), 'C', new ItemStack(Item.ingotAncientMetal));
        event.registerShapedRecipe(new ItemStack(crafting_box, 1, 6), false, " A ", " B ", " C ", 'A', new ItemStack(Item.enderPearl), 'B', new ItemStack(Block.workbench, 1, 9), 'C', new ItemStack(Item.ingotMithril));
        event.registerShapedRecipe(new ItemStack(crafting_box, 1, 7), false, " A ", " B ", " C ", 'A', new ItemStack(Item.enderPearl), 'B', new ItemStack(Block.workbench, 1, 10), 'C', new ItemStack(Item.ingotAdamantium));

        event.registerShapedRecipe(new ItemStack(storage_panel), false, "ABA", "BCB", "ABA", 'A', new ItemStack(Block.wood), 'B', new ItemStack(Item.stick), 'C', new ItemStack(Block.planks));
        event.registerShapedRecipe(new ItemStack(storage_cable, 16), false, "ABA", "BBB", "ABA", 'A', new ItemStack(Block.wood), 'B', new ItemStack(Item.stick));


        registerCompatCraftingBoxRecipe(event, EZCraftingBoxCompat.TIER_NICKEL);
        registerCompatCraftingBoxRecipe(event, EZCraftingBoxCompat.TIER_TUNGSTEN);
        registerCompatCraftingBoxRecipe(event, EZCraftingBoxCompat.TIER_VIBRANIUM);
        registerCompatCraftingBoxRecipe(event, EZCraftingBoxCompat.TIER_INFINITY);
    }

    private static void registerCompatCraftingBoxRecipe(RecipeRegistryEvent event, int tier)
    {
        Item ingot = EZCraftingBoxCompat.getIngotForTier(tier);

        if (ingot == null)
        {
            return;
        }

        event.registerShapedRecipe(new ItemStack(crafting_box, 1, tier), false, " A ", " B ", " C ", 'A', new ItemStack(Item.enderPearl), 'B', new ItemStack(Block.workbench, 1, getWorkbenchSubtypeForCompatTier(tier)), 'C', new ItemStack(ingot));
    }

    private static int getWorkbenchSubtypeForCompatTier(int tier)
    {
        return switch (tier)
        {
            // nickel is the iron-equivalent step
            case EZCraftingBoxCompat.TIER_NICKEL -> 7;
            // tungsten sits between mithril and adamantium
            case EZCraftingBoxCompat.TIER_TUNGSTEN -> 9;
            // vibranium and above use adamantium workbench
            default -> 10;
        };
    }

}