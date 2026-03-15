package com.zerofall.ezstorage.client.renderer;

import com.zerofall.ezstorage.block.BlockStorageCable;
import net.minecraft.Block;
import net.minecraft.EntityRenderer;
import net.minecraft.IBlockAccess;
import net.minecraft.Icon;
import net.minecraft.RenderBlocks;
import net.minecraft.Tessellator;
import org.lwjgl.opengl.GL11;

public final class StorageCableBlockRenderer {

    private StorageCableBlockRenderer() {}

    public static boolean renderWorldBlock(RenderBlocks renderer, Block block, IBlockAccess world, int x, int y, int z) {

        Icon icon = block.getIcon(0, world.getBlockMetadata(x, y, z));

        int color = block.colorMultiplier(world, x, y, z);
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;

        if (EntityRenderer.anaglyphEnable) {
            float grayscaleRed = (red * 30.0F + green * 59.0F + blue * 11.0F) / 100.0F;
            float grayscaleGreen = (red * 30.0F + green * 70.0F) / 100.0F;
            float grayscaleBlue = (red * 30.0F + blue * 70.0F) / 100.0F;
            red = grayscaleRed;
            green = grayscaleGreen;
            blue = grayscaleBlue;
        }

        renderSegment(renderer, block, x, y, z, BlockStorageCable.CORE_MIN, BlockStorageCable.CORE_MIN, BlockStorageCable.CORE_MIN,
                BlockStorageCable.CORE_MAX, BlockStorageCable.CORE_MAX, BlockStorageCable.CORE_MAX, icon, red, green, blue);

        if (BlockStorageCable.canConnectToBlock(world, x, y - 1, z)) {
            renderSegment(renderer, block, x, y, z, BlockStorageCable.CORE_MIN, 0.0F, BlockStorageCable.CORE_MIN,
                    BlockStorageCable.CORE_MAX, BlockStorageCable.CORE_MIN, BlockStorageCable.CORE_MAX, icon, red, green, blue);
        }

        if (BlockStorageCable.canConnectToBlock(world, x, y + 1, z)) {
            renderSegment(renderer, block, x, y, z, BlockStorageCable.CORE_MIN, BlockStorageCable.CORE_MAX, BlockStorageCable.CORE_MIN,
                    BlockStorageCable.CORE_MAX, 1.0F, BlockStorageCable.CORE_MAX, icon, red, green, blue);
        }

        if (BlockStorageCable.canConnectToBlock(world, x, y, z - 1)) {
            renderSegment(renderer, block, x, y, z, BlockStorageCable.CORE_MIN, BlockStorageCable.CORE_MIN, 0.0F,
                    BlockStorageCable.CORE_MAX, BlockStorageCable.CORE_MAX, BlockStorageCable.CORE_MIN, icon, red, green, blue);
        }

        if (BlockStorageCable.canConnectToBlock(world, x, y, z + 1)) {
            renderSegment(renderer, block, x, y, z, BlockStorageCable.CORE_MIN, BlockStorageCable.CORE_MIN, BlockStorageCable.CORE_MAX,
                    BlockStorageCable.CORE_MAX, BlockStorageCable.CORE_MAX, 1.0F, icon, red, green, blue);
        }

        if (BlockStorageCable.canConnectToBlock(world, x - 1, y, z)) {
            renderSegment(renderer, block, x, y, z, 0.0F, BlockStorageCable.CORE_MIN, BlockStorageCable.CORE_MIN,
                    BlockStorageCable.CORE_MIN, BlockStorageCable.CORE_MAX, BlockStorageCable.CORE_MAX, icon, red, green, blue);
        }

        if (BlockStorageCable.canConnectToBlock(world, x + 1, y, z)) {
            renderSegment(renderer, block, x, y, z, BlockStorageCable.CORE_MAX, BlockStorageCable.CORE_MIN, BlockStorageCable.CORE_MIN,
                    1.0F, BlockStorageCable.CORE_MAX, BlockStorageCable.CORE_MAX, icon, red, green, blue);
        }

        return true;
    }

    private static void renderSegment(RenderBlocks renderer, Block block, int x, int y, int z, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Icon icon, float red, float green, float blue) {
        renderer.setRenderBounds(minX, minY, minZ, maxX, maxY, maxZ);
        renderer.renderStandardBlockWithColorMultiplier(block, x, y, z, red, green, blue);
    }

    public static void renderInventoryBlock(RenderBlocks renderer, Block block, int metadata) {
        Icon icon = block.getIcon(0, metadata);


        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

        renderInventoryCuboid(renderer, block, icon,
                0.0F, BlockStorageCable.CORE_MIN, BlockStorageCable.CORE_MIN,
                1.0F, BlockStorageCable.CORE_MAX, BlockStorageCable.CORE_MAX);

        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        block.setBlockBoundsForItemRender(metadata);
    }

    private static void renderInventoryCuboid(RenderBlocks renderer, Block block, Icon icon, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        Tessellator tessellator = Tessellator.instance;
        renderer.setRenderBounds(minX, minY, minZ, maxX, maxY, maxZ);

        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, -1.0F, 0.0F);
        renderer.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, icon);
        tessellator.draw();

        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 1.0F, 0.0F);
        renderer.renderFaceYPos(block, 0.0D, 0.0D, 0.0D, icon);
        tessellator.draw();

        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 0.0F, -1.0F);
        renderer.renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, icon);
        tessellator.draw();

        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 0.0F, 1.0F);
        renderer.renderFaceZPos(block, 0.0D, 0.0D, 0.0D, icon);
        tessellator.draw();

        tessellator.startDrawingQuads();
        tessellator.setNormal(-1.0F, 0.0F, 0.0F);
        renderer.renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, icon);
        tessellator.draw();

        tessellator.startDrawingQuads();
        tessellator.setNormal(1.0F, 0.0F, 0.0F);
        renderer.renderFaceXPos(block, 0.0D, 0.0D, 0.0D, icon);
        tessellator.draw();
    }
}
