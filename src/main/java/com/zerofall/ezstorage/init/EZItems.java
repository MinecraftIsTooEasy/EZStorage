//package com.zerofall.ezstorage.init;
//
//import net.minecraft.Block;
//import net.minecraft.Item;
//import net.minecraft.ItemStack;
//import net.minecraftforge.oredict.ShapedOreRecipe;
//
//
//import cpw.mods.fml.common.registry.GameRegistry;
//import net.xiaoyu233.fml.reload.event.RecipeRegistryEvent;
//
//public class EZItems {
//
//    public static Item portable_storage_panel;
//
//    public static void register() {
//        GameRegistry.registerItem(
//            portable_storage_panel,
//            portable_storage_panel.getUnlocalizedName()
//                .substring(5));
//    }
//
//    public static void registerRecipes(RecipeRegistryEvent event) {
//        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(portable_storage_panel), "ABA", "BCB", "DBD", 'A', Block.torchRedstoneActive, 'B', "slabWood", 'C', EZBlocks.storage_core, 'D', "ingotGold"));
//    }
//}
