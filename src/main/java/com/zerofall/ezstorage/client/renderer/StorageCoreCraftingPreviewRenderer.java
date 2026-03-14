package com.zerofall.ezstorage.client.renderer;

import java.util.HashMap;
import java.util.Map;

import com.zerofall.ezstorage.container.ContainerStorageCoreCrafting;
import com.zerofall.ezstorage.init.EZBlocks;
import com.zerofall.ezstorage.tileentity.TileEntityStorageCore;
import com.zerofall.ezstorage.util.EZInventory;

import net.minecraft.Block;
import net.minecraft.EntityPlayer;
import net.minecraft.Icon;
import net.minecraft.Item;
import net.minecraft.ItemRenderer;
import net.minecraft.ItemStack;
import net.minecraft.Minecraft;
import net.minecraft.RenderBlocks;
import net.minecraft.Tessellator;
import net.minecraft.TileEntity;
import net.minecraft.TileEntityRenderer;
import net.minecraft.TileEntitySpecialRenderer;
import net.minecraft.TextureManager;
import net.minecraft.TextureMap;
import net.minecraft.World;
import org.lwjgl.opengl.GL11;

/**
 * Renders 3x3 crafting ingredients above the linked crafting box.
 */
public class StorageCoreCraftingPreviewRenderer extends TileEntitySpecialRenderer {

    private static final double ITEM_SPACING = 3.0 / 16.0;
    private static final double ITEM_OFFSET = 0.3125;
    private static final double ITEM_VISUALWORKBENCH_Y = 1.09375;
    private static final double ROTATION_SNAP_RANGE = 3.0D;
    private static final double ROTATION_OFFSET_RADIANS = 3.9269908169872414D;
    private static final Map<Long, RotationState> ROTATION_STATES = new HashMap<Long, RotationState>();
    private static final RenderBlocks PREVIEW_RENDER_BLOCKS = new RenderBlocks();

    private static class RotationState
    {
        long ticks;
        float currentAngle;
        float nextAngle;
        int sector = Integer.MIN_VALUE;
        boolean animating;
        float animationAngleStart;
        float animationAngleEnd;
        long startTicks;
        double playerAngle;
    }

    @Override
    public void renderTileEntityAt(TileEntity tileEntity, double x, double y, double z, float partialTicks)
    {
        // World-space rendering is handled by RenderGlobalMixin to keep preview always visible.
    }

    public static int renderCorePreviewWorld(TileEntityStorageCore core, float partialTicks)
    {
        World world = core.getWorldObj();

        if (world == null || !core.hasCraftBox)
        {
            return 0;
        }

        int craftBoxX = core.craftBoxX;
        int craftBoxY = core.craftBoxY;
        int craftBoxZ = core.craftBoxZ;

        if (world.getBlock(craftBoxX, craftBoxY, craftBoxZ) != EZBlocks.crafting_box)
        {
            return 0;
        }

        double x = craftBoxX - TileEntityRenderer.staticPlayerX;
        double y = craftBoxY - TileEntityRenderer.staticPlayerY;
        double z = craftBoxZ - TileEntityRenderer.staticPlayerZ;
        return renderItemsAt(world, getPreviewMatrix(core), x, y, z, partialTicks);
    }

    public static int renderContainerPreviewWorld(ContainerStorageCoreCrafting container, float partialTicks)
    {
        if (container == null || container.worldObj == null)
        {
            return 0;
        }

        int x = container.craftBoxX;
        int y = container.craftBoxY;
        int z = container.craftBoxZ;

        if (container.worldObj.getBlock(x, y, z) != EZBlocks.crafting_box)
        {
            return 0;
        }

        ItemStack[] matrix = new ItemStack[9];

        for (int i = 0; i < 9; i++)
        {
            matrix[i] = container.craftMatrix.getStackInSlot(i);
        }

        int nonNull = 0;

        for (int i = 0; i < 9; i++)
        {
            if (matrix[i] != null)
            {
                nonNull++;
            }
        }

        double relX = x - TileEntityRenderer.staticPlayerX;
        double relY = y - TileEntityRenderer.staticPlayerY;
        double relZ = z - TileEntityRenderer.staticPlayerZ;
        return renderItemsAt(container.worldObj, matrix, relX, relY, relZ, partialTicks);
    }

    private static ItemStack[] getPreviewMatrix(TileEntityStorageCore core)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc != null
                && mc.thePlayer != null
                && mc.thePlayer.openContainer instanceof ContainerStorageCoreCrafting container)
        {
            if (container.craftBoxX == core.craftBoxX
                    && container.craftBoxY == core.craftBoxY
                    && container.craftBoxZ == core.craftBoxZ)
            {
                ItemStack[] matrix = new ItemStack[9];

                for (int i = 0; i < 9; i++)
                {
                    matrix[i] = container.craftMatrix.getStackInSlot(i);
                }

                if (hasAnyStack(matrix))
                {
                    return matrix;
                }
            }
        }

        if (hasAnyStack(core.craftMatrixPreview))
        {
            return core.craftMatrixPreview;
        }

        EZInventory inventory = core.getInventory();

        if (inventory != null && inventory.craftMatrix != null)
        {
            return inventory.craftMatrix;
        }
        return null;
    }

    private static boolean hasAnyStack(ItemStack[] matrix)
    {
        if (matrix == null)
        {
            return false;
        }

        for (int i = 0; i < matrix.length; i++)
        {
            if (matrix[i] != null)
            {
                return true;
            }
        }

        return false;
    }

    private static int renderItemsAt(World world, ItemStack[] matrix, double craftBoxRelX, double craftBoxRelY, double craftBoxRelZ, float partialTicks)
    {
        if (matrix == null)
        {
            return 0;
        }

        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.getTextureManager() == null)
        {
            return 0;
        }

        TextureManager textureManager = mc.getTextureManager();

        long stateKey = (((long)world.provider.dimensionId & 0x3FFL) << 54)
                | (((long)Math.floor(craftBoxRelX + TileEntityRenderer.staticPlayerX) & 0x3FFFFFFL) << 28)
                | (((long)Math.floor(craftBoxRelY + TileEntityRenderer.staticPlayerY) & 0xFFFL) << 16)
                | ((long)Math.floor(craftBoxRelZ + TileEntityRenderer.staticPlayerZ) & 0xFFFFL);
        RotationState state = getOrCreateRotationState(stateKey);
        advanceRotationState(world, state, craftBoxRelX + TileEntityRenderer.staticPlayerX, craftBoxRelY + TileEntityRenderer.staticPlayerY, craftBoxRelZ + TileEntityRenderer.staticPlayerZ);
        float angleDeg = lerp(partialTicks, state.currentAngle, state.nextAngle);
        int rendered = 0;

        for (int i = 0; i < 9; i++)
        {
            ItemStack stack = matrix[i];

            if (stack == null)
            {
                continue;
            }

            ItemStack renderStack = stack.copy();
            renderStack.stackSize = 1;

            int col = i % 3;
            int row = i / 3;

            double localX = col * ITEM_SPACING + ITEM_OFFSET - 0.5;
            double localZ = row * ITEM_SPACING + ITEM_OFFSET - 0.5;

            // Matches VisualWorkbench floating mode: 0.0 to 0.0125 vertical drift per slot.
            float shift = (float)Math.abs(((state.ticks + partialTicks) * 50.0 + (i * 1000L)) % 5000L - 2500L) / 200000.0F;

            boolean blockItem = isBlockItem3d(renderStack);
            float scale = blockItem ? 0.12F : 0.18F;

            GL11.glPushMatrix();
            GL11.glTranslated(craftBoxRelX + 0.5, craftBoxRelY + ITEM_VISUALWORKBENCH_Y + shift, craftBoxRelZ + 0.5);
            GL11.glRotatef(angleDeg, 0.0F, 1.0F, 0.0F);
            GL11.glTranslated(localX, 0.0, localZ);
            GL11.glScalef(scale, scale, scale);
            // Counter one remaining handedness mismatch so item models are no longer mirrored.
            GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            GL11.glEnable(32826);

            if (renderFixedWorkbenchStyleItem(textureManager, renderStack))
            {
                rendered++;
            }

            GL11.glDisable(32826);
            GL11.glPopMatrix();
        }

        return rendered;
    }

    private static boolean renderFixedWorkbenchStyleItem(TextureManager textureManager, ItemStack stack)
    {
        if (stack == null || stack.getItem() == null)
        {
            return false;
        }

        if (isBlockItem3d(stack))
        {
            int itemId = stack.itemID;
            Block block = Block.blocksList[itemId];
            int color = Item.itemsList[itemId].getColorFromItemStack(stack, 0);
            float red = (float)(color >> 16 & 255) / 255.0F;
            float green = (float)(color >> 8 & 255) / 255.0F;
            float blue = (float)(color & 255) / 255.0F;

            textureManager.bindTexture(TextureMap.locationBlocksTexture);
            GL11.glColor4f(red, green, blue, 1.0F);
            PREVIEW_RENDER_BLOCKS.renderBlockAsItem(block, stack.getItemSubtype(), 1.0F);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            return true;
        }

        Item item = stack.getItem();
        int itemId = stack.itemID;
        boolean renderedAny = false;
        int maxPass = item.requiresMultipleRenderPasses() ? 1 : 0;

        for (int pass = 0; pass <= maxPass; pass++)
        {
            Icon icon = item.requiresMultipleRenderPasses()
                    ? item.getIconFromSubtypeForRenderPass(stack.getItemSubtype(), pass)
                    : stack.getIconIndex();

            if (icon == null)
            {
                continue;
            }

            int color = Item.itemsList[itemId].getColorFromItemStack(stack, pass);
            float red = (float)(color >> 16 & 255) / 255.0F;
            float green = (float)(color >> 8 & 255) / 255.0F;
            float blue = (float)(color & 255) / 255.0F;

            textureManager.bindTexture(textureManager.getResourceLocation(stack.getItemSpriteNumber()));

            GL11.glPushMatrix();
            GL11.glColor4f(red, green, blue, 1.0F);
            GL11.glTranslatef(-0.5F, -0.5F, -0.03125F);

            ItemRenderer.renderItemIn2D(Tessellator.instance,
                    icon.getMaxU(),
                    icon.getMinV(),
                    icon.getMinU(),
                    icon.getMaxV(),
                    icon.getIconWidth(),
                    icon.getIconHeight(),
                    0.0625F);

            GL11.glPopMatrix();
            renderedAny = true;
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        return renderedAny;
    }

    private static boolean isBlockItem3d(ItemStack stack)
    {
        int itemId = stack.itemID;

        return stack.getItemSpriteNumber() == 0
                && itemId >= 0
                && itemId < Block.blocksList.length
                && Block.blocksList[itemId] != null
                && RenderBlocks.renderItemIn3d(Block.blocksList[itemId].getRenderType());
    }

    private static RotationState getOrCreateRotationState(long key)
    {
        RotationState state = ROTATION_STATES.get(key);

        if (state == null)
        {
            state = new RotationState();
            ROTATION_STATES.put(key, state);
        }

        return state;
    }

    private static void advanceRotationState(World world, RotationState state, double blockX, double blockY, double blockZ)
    {
        long worldTicks = world.getTotalWorldTime();

        if (state.ticks == 0L)
        {
            state.ticks = worldTicks;
        }

        while (state.ticks < worldTicks)
        {
            state.ticks++;
            updateRotationTick(state, blockX, blockY, blockZ);
        }

        if (state.ticks == worldTicks)
        {
            updateRotationTick(state, blockX, blockY, blockZ);
        }
    }

    private static void updateRotationTick(RotationState state, double blockX, double blockY, double blockZ)
    {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = null;
        double centerX = blockX + 0.5D;
        double centerY = blockY + 0.5D;
        double centerZ = blockZ + 0.5D;

        if (mc != null)
        {
            EntityPlayer localPlayer = mc.thePlayer;

            if (localPlayer != null)
            {
                double dx = localPlayer.posX - centerX;
                double dy = localPlayer.posY - centerY;
                double dz = localPlayer.posZ - centerZ;
                double distanceSq = dx * dx + dy * dy + dz * dz;

                if (distanceSq <= ROTATION_SNAP_RANGE * ROTATION_SNAP_RANGE)
                {
                    player = localPlayer;
                }
            }

            if (player == null && mc.theWorld != null)
            {
                player = mc.theWorld.getClosestPlayer(centerX, centerY, centerZ, ROTATION_SNAP_RANGE, false);
            }
        }

        if (player != null)
        {
            double dx = player.posX - centerX;
            double dz = player.posZ - centerZ;
            state.playerAngle = (Math.atan2(-dx, -dz) + ROTATION_OFFSET_RADIANS) % (Math.PI * 2.0D);
        }

        int sector = (int)(state.playerAngle * 2.0D / Math.PI);

        if (state.sector != sector)
        {
            state.animating = true;
            state.animationAngleStart = state.currentAngle;
            float delta1 = sector * 90.0F - state.currentAngle;
            float abs1 = Math.abs(delta1);
            float delta2 = delta1 + 360.0F;
            float abs2 = Math.abs(delta2);
            float delta3 = delta1 - 360.0F;
            float abs3 = Math.abs(delta3);

            if (abs3 < abs1 && abs3 < abs2)
            {
                state.animationAngleEnd = delta3 + state.currentAngle;
            }
            else if (abs2 < abs1 && abs2 < abs3)
            {
                state.animationAngleEnd = delta2 + state.currentAngle;
            }
            else
            {
                state.animationAngleEnd = delta1 + state.currentAngle;
            }

            state.startTicks = state.ticks;
            state.sector = sector;
        }

        if (state.animating)
        {
            if (state.ticks >= state.startTicks + 20L)
            {
                state.animating = false;
                float wrapped = (state.animationAngleEnd + 360.0F) % 360.0F;
                state.currentAngle = wrapped;
                state.nextAngle = wrapped;
            }
            else
            {
                state.currentAngle = (easeOutQuad(state.ticks - state.startTicks, state.animationAngleStart,
                        state.animationAngleEnd - state.animationAngleStart, 20.0D) + 360.0F) % 360.0F;
                state.nextAngle = (easeOutQuad(Math.min(state.ticks + 1L - state.startTicks, 20L), state.animationAngleStart,
                        state.animationAngleEnd - state.animationAngleStart, 20.0D) + 360.0F) % 360.0F;

                if (state.currentAngle != 0.0F || state.nextAngle != 0.0F)
                {
                    if (state.currentAngle == 0.0F && state.nextAngle >= 180.0F)
                    {
                        state.currentAngle = 360.0F;
                    }

                    if (state.nextAngle == 0.0F && state.currentAngle >= 180.0F)
                    {
                        state.nextAngle = 360.0F;
                    }
                }
            }
        }
    }

    private static float easeOutQuad(long t, float b, float c, double d)
    {
        double td = t / d;
        return (float)(-c * td * (td - 2.0D) + b);
    }

    private static float lerp(float partialTicks, float start, float end)
    {
        return start + (end - start) * partialTicks;
    }
}