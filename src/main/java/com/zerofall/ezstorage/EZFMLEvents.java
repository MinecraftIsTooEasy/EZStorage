package com.zerofall.ezstorage;

import com.google.common.eventbus.Subscribe;
import com.zerofall.ezstorage.init.EZBlocks;
import net.xiaoyu233.fml.reload.event.BlockRegistryEvent;
import net.xiaoyu233.fml.reload.event.RecipeRegistryEvent;

public class EZFMLEvents {


    @Subscribe
    public void onBlockRegister(BlockRegistryEvent event) {
        EZBlocks.registerBlocks(event);
    }

    @Subscribe
    public void onRecipeRegister(RecipeRegistryEvent event) {
        EZBlocks.registerRecipes(event);
    }
}