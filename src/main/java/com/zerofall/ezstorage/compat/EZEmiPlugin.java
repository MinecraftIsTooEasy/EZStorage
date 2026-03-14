package com.zerofall.ezstorage.compat;

import com.zerofall.ezstorage.init.EZBlocks;
import com.zerofall.ezstorage.container.ContainerStorageCoreCrafting;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.Block;
import net.minecraft.Item;
import net.minecraft.ItemStack;
import shims.java.net.minecraft.text.Text;

import java.util.List;

public class EZEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipeHandler(ContainerStorageCoreCrafting.class, new EZEmiStorageCraftingHandler());
        registerInfos(registry);
    }

    private void registerInfos(EmiRegistry registry) {
        infoBlock(registry, EZBlocks.storage_core, "ezstorage.emi.info.storage_core");
        infoBlock(registry, EZBlocks.storage_panel, "ezstorage.emi.info.storage_panel");
        infoBlock(registry, EZBlocks.storage_box, "ezstorage.emi.info.storage_box");
        infoBlock(registry, EZBlocks.condensed_storage_box, "ezstorage.emi.info.condensed_storage_box");
        infoBlock(registry, EZBlocks.hyper_storage_box, "ezstorage.emi.info.hyper_storage_box");
        infoBlock(registry, EZBlocks.input_port, "ezstorage.emi.info.input_port");
        infoBlock(registry, EZBlocks.storage_cable, "ezstorage.emi.info.storage_cable");
        infoBlockMetaRange(registry, EZBlocks.crafting_box, 0, 7, "ezstorage.emi.info.crafting_box");
    }

    private void infoBlock(EmiRegistry registry, Block block, String infoKey) {
        if (block == null) {
            return;
        }

        Item item = Item.itemsList[block.blockID];

        if (item == null) {
            return;
        }

        registry.addRecipe(new EmiInfoRecipe(
            List.of(EmiStack.of(item)),
            List.of(Text.translatable(infoKey)),
            null
        ));
    }

    private void infoBlockMetaRange(EmiRegistry registry, Block block, int minMeta, int maxMeta, String infoKey) {
        if (block == null) {
            return;
        }

        Item item = Item.itemsList[block.blockID];

        if (item == null) {
            return;
        }

        for (int meta = minMeta; meta <= maxMeta; meta++) {
            registry.addRecipe(new EmiInfoRecipe(
                List.of(EmiStack.of(new ItemStack(item, 1, meta))),
                List.of(Text.translatable(infoKey)),
                null
            ));
        }
    }
}