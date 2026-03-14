package com.zerofall.ezstorage.mixin;

import com.zerofall.ezstorage.block.BlockCraftingBox;
import com.zerofall.ezstorage.compat.EZCraftingBoxCompat;
import com.zerofall.ezstorage.container.ContainerStorageCoreCrafting;
import net.minecraft.ClientPlayer;
import net.minecraft.Container;
import net.minecraft.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayer.class)
public class ClientPlayerMixin {

    @Inject(method = "getBenchAndToolsModifier", at = @At("HEAD"), cancellable = true)
    private void ezstorage$applyCraftingBoxBenchModifier(Container container, CallbackInfoReturnable<Float> cir)
    {
        if (!(container instanceof ContainerStorageCoreCrafting craftingContainer))
        {
            return;
        }

        Material terminalMaterial = BlockCraftingBox.getToolMaterialForTier(craftingContainer.getCraftingTier());
        cir.setReturnValue(EZCraftingBoxCompat.getBenchAndToolsModifierForMaterial(terminalMaterial));
    }
}