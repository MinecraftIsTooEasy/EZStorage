package com.zerofall.ezstorage.mixin;

import com.zerofall.ezstorage.client.renderer.StorageCableBlockRenderer;
import com.zerofall.ezstorage.init.EZBlocks;
import net.minecraft.Block;
import net.minecraft.IBlockAccess;
import net.minecraft.RenderBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderBlocks.class)
public abstract class RenderBlocksMixin {

    @Shadow public IBlockAccess blockAccess;

    @Inject(method = "renderBlockByRenderType", at = @At("HEAD"), cancellable = true)
    private void ezstorage$renderConnectedCable(Block block, int x, int y, int z, CallbackInfoReturnable<Boolean> cir)
    {
        if (block != EZBlocks.storage_cable || this.blockAccess == null) {
            return;
        }

        RenderBlocks renderer = (RenderBlocks) (Object) this;
        cir.setReturnValue(StorageCableBlockRenderer.renderWorldBlock(renderer, block, this.blockAccess, x, y, z));
    }

    @Inject(method = "renderBlockAsItem", at = @At("HEAD"), cancellable = true)
    private void ezstorage$renderCableAsItem(Block block, int metadata, float brightness, CallbackInfo ci) {
        if (block != EZBlocks.storage_cable) {
            return;
        }

        RenderBlocks renderer = (RenderBlocks) (Object) this;
        StorageCableBlockRenderer.renderInventoryBlock(renderer, block, metadata);
        ci.cancel();
    }
}
