package com.zerofall.ezstorage.mixin;

import com.zerofall.ezstorage.block.BlockCraftingBox;
import com.zerofall.ezstorage.container.ContainerStorageCoreCrafting;
import com.zerofall.ezstorage.init.EZBlocks;
import com.zerofall.ezstorage.tileentity.TileEntityStorageCore;
import com.zerofall.ezstorage.util.BlockRef;
import com.zerofall.ezstorage.util.EZInventory;
import com.zerofall.ezstorage.util.EZInventoryManager;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts Ctrl+right-click on a crafting box to perform a tier upgrade,
 * cancelling the packet so the GUI is never opened during an upgrade attempt.
 * Also intercepts normal right-click on a crafting box to open the GUI via
 * our custom S2C packets instead of the vanilla Packet100OpenWindow path,
 * preventing the TileEntityFurnace cast crash.
 */
@Mixin(NetServerHandler.class)
public class NetServerHandlerMixin {

    @Inject(method = "handleSimpleSignal", at = @At("HEAD"), cancellable = true)
    private void ezstorage$handleCraftingCompleted(Packet85SimpleSignal packet, CallbackInfo ci)
    {
        ServerPlayer player = ((NetServerHandler)(Object)this).playerEntity;

        if (ezstorage$denyBreakingNonEmptyCore(packet, player))
        {
            ci.cancel();
            return;
        }

        if (packet.signal_type != EnumSignal.crafting_completed)
        {
            return;
        }

        if (!(player.openContainer instanceof ContainerStorageCoreCrafting container))
        {
            return;
        }

        container.syncCraftMatrixFromInventory();

        // In this container layout: 45 storage + 36 player inventory = crafting output slot at index 81.
        final int craftingOutputSlotIndex = 81;

        if (container.inventorySlots == null || container.inventorySlots.size() <= craftingOutputSlotIndex)
        {
            ci.cancel();
            return;
        }

        Slot slot = container.getSlot(craftingOutputSlotIndex);

        if (container.isCurrentRecipeBlockedByTier())
        {
            container.detectAndSendChanges();
            ci.cancel();
            return;
        }

        if (slot instanceof SlotCrafting && slot.getHasStack())
        {
            ItemStack itemStack = slot.getStack();
            slot.onPickupFromSlot(player, itemStack);
            player.addExperience(-packet.getInteger());
            container.saveGrid();
            EZInventoryManager.sendToClients(container.inventory);
            container.detectAndSendChanges();
        }

        // Prevent vanilla handler from using slot 0 logic, which doesn't apply to this custom container.
        ci.cancel();
    }

    @Unique
    private static boolean ezstorage$denyBreakingNonEmptyCore(Packet85SimpleSignal packet, ServerPlayer player)
    {
        EnumSignal signalType = packet.signal_type;

        if (signalType != EnumSignal.digging_block_start && signalType != EnumSignal.digging_block_complete)
        {
            return false;
        }

        World world = player.worldObj;

        if (world == null)
        {
            return false;
        }

        int x = packet.getBlockX();
        int y = packet.getBlockY();
        int z = packet.getBlockZ();

        if (world.getBlock(x, y, z) != EZBlocks.storage_core)
        {
            return false;
        }

        TileEntity tileEntity = world.getBlockTileEntity(x, y, z);

        if (!(tileEntity instanceof TileEntityStorageCore core) || !core.hasStoredItems())
        {
            return false;
        }

        if (signalType == EnumSignal.digging_block_start)
        {
            player.sendChatToPlayer(ChatMessageComponent.createFromTranslationKey("chat.msg.storagecore_break_blocked_nonempty").setColor(EnumChatFormatting.YELLOW));
        }

        player.playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(x, y, z, world));
        return true;
    }

    @Inject(method = "handleRightClick", at = @At("HEAD"), cancellable = true)
    private void ezstorage$onHandleRightClick(Packet81RightClick packet, CallbackInfo ci)
    {
        ServerPlayer player = ((NetServerHandler) (Object) this).playerEntity;
        World world = player.worldObj;

        if (world.isRemote) return;

        int[] target = ezstorage$getTargetBlock(player, world);

        if (target == null) return;

        int bx = target[0];
        int by = target[1];
        int bz = target[2];

        Block block = world.getBlock(bx, by, bz);

        if (block != EZBlocks.crafting_box) return;

        if (packet.ctrl_is_down)
        {
            ItemStack stack = ezstorage$getHeldStack(player);

            if (stack != null && stack.getItem() != null)
            {
                Item heldItem = stack.getItem();
                int curTier = world.getBlockMetadata(bx, by, bz);

                int ingotGroup = BlockCraftingBox.getIngotGroup(heldItem);

                if (ingotGroup != -1)
                {
                    int curGroup = BlockCraftingBox.getTierGroup(curTier);
                    int maxGroup = BlockCraftingBox.getMaxUpgradeGroup();

                    if (curGroup >= maxGroup)
                    {
                        player.sendChatToPlayer(
                            ChatMessageComponent.createFromTranslationKey("ezstorage.crafting_box.upgrade.maxed").setColor(EnumChatFormatting.YELLOW));
                        ci.cancel();
                        return;
                    }

                    if (ingotGroup != curGroup + 1)
                    {
                        String requiredName = BlockCraftingBox.getNeedGroupText(curGroup + 1);

                        player.sendChatToPlayer(
                            ChatMessageComponent.createFromText(
                                    StatCollector.translateToLocalFormatted("ezstorage.crafting_box.upgrade.wrong_tier", requiredName)).setColor(EnumChatFormatting.YELLOW));
                        ci.cancel();
                        return;
                    }

                    int newTier = BlockCraftingBox.getTierForIngot(heldItem);
                    ezstorage$doUpgrade(world, bx, by, bz, player, stack, newTier);
                    ci.cancel();
                    return;
                }
            }
        }

        ezstorage$openGui(world, bx, by, bz, player);
        ci.cancel();
    }

    @Unique
    private void ezstorage$openGui(World world, int bx, int by, int bz, ServerPlayer player)
    {
        TileEntityStorageCore core;
        TileEntity tileEntity = world.getBlockTileEntity(bx, by, bz);

        if (tileEntity instanceof TileEntityStorageCore)
        {
            core = (TileEntityStorageCore) tileEntity;
        }
        else
        {
            BlockCraftingBox craftingBox = (BlockCraftingBox) EZBlocks.crafting_box;
            BlockRef blockRef = new BlockRef(craftingBox, bx, by, bz);
            core = craftingBox.findCore(blockRef, world, null);
        }

        if (core == null)
        {
            player.sendChatToPlayer(ChatMessageComponent.createFromTranslationKey("chat.msg.storagecore_not_found"));
            return;
        }

        EZInventory inventory = core.getInventory();
        if (inventory != null)
        {
            BlockCraftingBox craftingBox = (BlockCraftingBox) EZBlocks.crafting_box;
            craftingBox.openPlayerInventoryGui(player, inventory, world, bx, by, bz, core);
        }
    }

    @Unique
    private void ezstorage$doUpgrade(World world, int bx, int by, int bz, ServerPlayer player, ItemStack held, int newTier)
    {
        world.setBlockMetadataWithNotify(bx, by, bz, newTier, 3);

        if (!player.capabilities.isCreativeMode)
        {
            held.stackSize--;

            if (held.stackSize <= 0)
            {
                player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            }
        }

        String tierName = BlockCraftingBox.getTierName(newTier);
        player.sendChatToPlayer(
            ChatMessageComponent.createFromText(
                    StatCollector.translateToLocalFormatted("ezstorage.crafting_box.upgrade.success", tierName)).setColor(EnumChatFormatting.GREEN));
        world.playSoundEffect(bx + 0.5, by + 0.5, bz + 0.5, "random.pop", 0.8F, 1.0F);
    }

    @Unique
    private static int[] ezstorage$getTargetBlock(ServerPlayer player, World world)
    {
        double reach = 5.0D;
        double yawRad   = Math.toRadians(player.rotationYaw);
        double pitchRad = Math.toRadians(player.rotationPitch);
        double cosPitch = Math.cos(pitchRad);
        double dirX = -Math.sin(yawRad) * cosPitch;
        double dirY = -Math.sin(pitchRad);
        double dirZ =  Math.cos(yawRad) * cosPitch;

        double sx = player.posX;
        double sy = player.posY + player.getEyeHeight();
        double sz = player.posZ;

        for (double d = 0.0; d <= reach; d += 0.5)
        {
            int bx = (int) Math.floor(sx + dirX * d);
            int by = (int) Math.floor(sy + dirY * d);
            int bz = (int) Math.floor(sz + dirZ * d);

            if (!world.isAirBlock(bx, by, bz))
            {
                return new int[]{ bx, by, bz };
            }
        }

        return null;
    }

    @Unique
    private static ItemStack ezstorage$getHeldStack(ServerPlayer player)
    {
        int idx = player.inventory.currentItem;
        return player.inventory.mainInventory[idx];
    }

}
