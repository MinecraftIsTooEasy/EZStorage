package com.zerofall.ezstorage.container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.minecraft.EntityPlayer;
import net.minecraft.IInventory;
import net.minecraft.InventoryCraftResult;
import net.minecraft.InventoryCrafting;
import net.minecraft.Slot;
import net.minecraft.SlotCrafting;
import net.minecraft.ItemStack;
import net.minecraft.IRecipe;
import net.minecraft.CraftingManager;
import net.minecraft.CraftingResult;
import net.minecraft.EntityClientPlayerMP;
import net.minecraft.EntityOtherPlayerMP;
import net.minecraft.EnumEntityFX;
import net.minecraft.EnumSignal;
import net.minecraft.Minecraft;
import net.minecraft.Packet85SimpleSignal;
import net.minecraft.Packet90BroadcastToAssociatedPlayers;
import net.minecraft.World;
import net.minecraft.Material;
import net.minecraft.ServerPlayer;

import com.zerofall.ezstorage.block.BlockCraftingBox;
import com.zerofall.ezstorage.configuration.EZConfiguration;
import com.zerofall.ezstorage.init.EZBlocks;

import com.zerofall.ezstorage.util.EZInventory;
import com.zerofall.ezstorage.util.EZInventoryManager;

public class ContainerStorageCoreCrafting extends ContainerStorageCore {

    private static final class CraftingProgressSnapshot {
        int dim;
        int x;
        int y;
        int z;
        int matrixHash;
        int resultItemId;
        int resultItemDamage;
        int craftingTicks;
        int craftingPeriod;
        long savedAtMs;
    }

    private static final Map<Long, CraftingProgressSnapshot> CLIENT_PROGRESS_SNAPSHOTS = new HashMap<Long, CraftingProgressSnapshot>();
    private static final long SNAPSHOT_MAX_AGE_MS = 5L * 60L * 1000L;

    public InventoryCrafting craftMatrix = new InventoryCrafting(this, 3, 3);
    public IInventory craftResult = new InventoryCraftResult();
    /** Exposed for GUI tooltip rendering (mirrors MITEContainerCrafting.current_crafting_result). */
    public CraftingResult current_crafting_result;
    private CraftingResult previous_crafting_result;
    /** Public so GuiCraftingCore can read the world when its own world field is null. */
    public World worldObj;
    public int craftBoxX;
    public int craftBoxY;
    public int craftBoxZ;
    private boolean hasCraftBoxPos;
    private boolean resumeProgressArmed;
    private int suppressCraftingResetDepth;
    private int resumeTicks;
    private int resumePeriod;

    public ContainerStorageCoreCrafting(EntityPlayer player, World world, EZInventory inventory)
    {
        this(player, world, 0, 0, 0, false);
        this.inventory = inventory;

        if (this.inventory != null && this.inventory.craftMatrix != null)
        {
            boolean loaded = false;

            for (int k = 0; k < 9; k++)
            {
                if (this.inventory.craftMatrix[k] != null)
                {
                    this.craftMatrix.setInventorySlotContents(k, this.inventory.craftMatrix[k]);
                    loaded = true;
                }
            }
            if (loaded)
            {
                this.onCraftMatrixChanged(this.craftMatrix);
            }
        }
    }

    public ContainerStorageCoreCrafting(EntityPlayer player, World world, EZInventory inventory, int craftBoxX, int craftBoxY, int craftBoxZ)
    {
        this(player, world, craftBoxX, craftBoxY, craftBoxZ, true);
        this.inventory = inventory;

        if (this.inventory != null && this.inventory.craftMatrix != null)
        {
            boolean loaded = false;

            for (int k = 0; k < 9; k++)
            {
                if (this.inventory.craftMatrix[k] != null)
                {
                    this.craftMatrix.setInventorySlotContents(k, this.inventory.craftMatrix[k]);
                    loaded = true;
                }
            }
            if (loaded)
            {
                this.onCraftMatrixChanged(this.craftMatrix);
            }
        }
    }

    public ContainerStorageCoreCrafting(EntityPlayer player, World world)
    {
        this(player, world, 0, 0, 0, false);
    }

    public ContainerStorageCoreCrafting(EntityPlayer player, World world, int craftBoxX, int craftBoxY, int craftBoxZ)
    {
        this(player, world, craftBoxX, craftBoxY, craftBoxZ, true);
    }

    private ContainerStorageCoreCrafting(EntityPlayer player, World world, int craftBoxX, int craftBoxY, int craftBoxZ, boolean hasCraftBoxPos)
    {
        super(player);

        this.worldObj = world;
        this.craftBoxX = craftBoxX;
        this.craftBoxY = craftBoxY;
        this.craftBoxZ = craftBoxZ;
        this.hasCraftBoxPos = hasCraftBoxPos;
        this.suppressCraftingResetDepth = 0;
        this.resumeTicks = -1;
        this.resumePeriod = -1;
        this.addSlotToContainer(new SlotCrafting(player, this.craftMatrix, this.craftResult, 0, 118, 132));
        int i;
        int j;

        for (i = 0; i < 3; ++i)
        {
            for (j = 0; j < 3; ++j)
            {
                this.addSlotToContainer(new Slot(this.craftMatrix, j + i * 3, 44 + j * 18, 114 + i * 18));
            }
        }

        this.onCraftMatrixChanged(this.craftMatrix);
    }

    public void onCraftMatrixChanged(IInventory inventoryIn)
    {
        this.current_crafting_result = CraftingManager.getInstance().findMatchingRecipe(this.craftMatrix, this.worldObj, this.player);

        if (!CraftingResult.haveEquivalentItemStacks(this.current_crafting_result, this.previous_crafting_result))
        {
            if (!this.isSuppressingCraftingReset())
            {
                this.player.clearCrafting();
            }
        }

        // Always refresh the output slot contents; equivalent result does not imply slot is still populated.
        this.refreshCraftingSlotResult();

        this.previous_crafting_result = this.current_crafting_result;

        tryRestoreCraftingProgressOnClient();
    }

    public void beginSuppressCraftingReset()
    {
        this.suppressCraftingResetDepth++;
    }

    public void endSuppressCraftingReset()
    {
        if (this.suppressCraftingResetDepth > 0)
        {
            this.suppressCraftingResetDepth--;
        }
    }

    public boolean isSuppressingCraftingReset()
    {
        return this.suppressCraftingResetDepth > 0;
    }

    private static long makeCraftBoxKey(int dim, int x, int y, int z)
    {
        long key = 1469598103934665603L;
        key = (key ^ dim) * 1099511628211L;
        key = (key ^ x) * 1099511628211L;
        key = (key ^ y) * 1099511628211L;
        key = (key ^ z) * 1099511628211L;
        return key;
    }

    private int computeMatrixHash()
    {
        int hash = 1;

        for (int i = 0; i < 9; i++)
        {
            ItemStack stack = this.craftMatrix.getStackInSlot(i);

            if (stack == null)
            {
                hash = 31 * hash;
                continue;
            }

            hash = 31 * hash + stack.itemID;
            hash = 31 * hash + stack.getItemSubtype();
            hash = 31 * hash + stack.stackSize;
        }

        return hash;
    }

    private void tryRestoreCraftingProgressOnClient()
    {
        if (!this.player.worldObj.isRemote || !(this.player instanceof EntityClientPlayerMP clientPlayer))
        {
            return;
        }

        if (this.worldObj == null || this.current_crafting_result == null || this.current_crafting_result.item_stack == null)
        {
            return;
        }

        if (this.resumeProgressArmed)
        {
            return;
        }

        long key = makeCraftBoxKey(this.worldObj.provider.dimensionId, this.craftBoxX, this.craftBoxY, this.craftBoxZ);
        CraftingProgressSnapshot snapshot = CLIENT_PROGRESS_SNAPSHOTS.get(key);

        if (snapshot == null)
        {
            return;
        }

        long now = System.currentTimeMillis();

        if (now - snapshot.savedAtMs > SNAPSHOT_MAX_AGE_MS)
        {
            CLIENT_PROGRESS_SNAPSHOTS.remove(key);
            return;
        }

        ItemStack resultStack = this.current_crafting_result.item_stack;

        if (snapshot.matrixHash != computeMatrixHash()
            || snapshot.resultItemId != resultStack.itemID
            || snapshot.resultItemDamage != resultStack.getItemSubtype())
        {
            // Container opens before S2C inventory sync; keep snapshot and retry next tick.
            return;
        }

        this.resumeProgressArmed = true;
        this.resumeTicks = Math.max(0, snapshot.craftingTicks);
        this.resumePeriod = Math.max(1, snapshot.craftingPeriod);
        clientPlayer.crafting_ticks = this.resumeTicks;
        clientPlayer.crafting_period = this.resumePeriod;
        clientPlayer.crafting_proceed = false;
        CLIENT_PROGRESS_SNAPSHOTS.remove(key);
    }

    private void captureCraftingProgressSnapshotOnClient()
    {
        if (!this.player.worldObj.isRemote || !(this.player instanceof EntityClientPlayerMP clientPlayer))
        {
            return;
        }

        if (this.worldObj == null)
        {
            return;
        }

        if (clientPlayer.crafting_ticks <= 0 || clientPlayer.crafting_period <= 0)
        {
            return;
        }

        CraftingResult result = this.current_crafting_result;

        if (result == null || result.item_stack == null)
        {
            result = CraftingManager.getInstance().findMatchingRecipe(this.craftMatrix, this.worldObj, this.player);
        }

        if (result == null || result.item_stack == null)
        {
            return;
        }

        CraftingProgressSnapshot snapshot = new CraftingProgressSnapshot();
        snapshot.dim = this.worldObj.provider.dimensionId;
        snapshot.x = this.craftBoxX;
        snapshot.y = this.craftBoxY;
        snapshot.z = this.craftBoxZ;
        snapshot.matrixHash = computeMatrixHash();
        snapshot.resultItemId = result.item_stack.itemID;
        snapshot.resultItemDamage = result.item_stack.getItemSubtype();
        snapshot.craftingTicks = clientPlayer.crafting_ticks;
        snapshot.craftingPeriod = clientPlayer.crafting_period;
        snapshot.savedAtMs = System.currentTimeMillis();

        long key = makeCraftBoxKey(snapshot.dim, snapshot.x, snapshot.y, snapshot.z);
        CLIENT_PROGRESS_SNAPSHOTS.put(key, snapshot);
    }

    private SlotCrafting getCraftingSlot()
    {
        return (SlotCrafting)this.getSlot(this.rowCount() * 9 + 36);
    }

    private void refreshCraftingSlotResult()
    {
        SlotCrafting craftingSlot = this.getCraftingSlot();

        if (craftingSlot == null)
        {
            return;
        }

        craftingSlot.crafting_result = this.current_crafting_result;

        if (this.current_crafting_result == null
            || this.current_crafting_result.item_stack == null
            || this.current_crafting_result.item_stack.getItem() == null)
        {
            craftingSlot.crafting_result_index = 0;
            this.craftResult.setInventorySlotContents(0, null);
            return;
        }

        this.craftResult.setInventorySlotContents(0, this.current_crafting_result.item_stack.copy());
        craftingSlot.checkCraftingResultIndex(this.player);
    }

    /** Mirrors MITEContainerCrafting.getRecipe() so GUI tooltip code can call it. */
    public IRecipe getRecipe() {
        return this.current_crafting_result == null ? null : this.current_crafting_result.recipe;
    }

    public int getCraftingTier()
    {
        if (!this.hasCraftBoxPos || this.worldObj == null)
        {
            return 0;
        }

        if (this.worldObj.getBlock(this.craftBoxX, this.craftBoxY, this.craftBoxZ) != EZBlocks.crafting_box)
        {
            return 0;
        }

        return this.worldObj.getBlockMetadata(this.craftBoxX, this.craftBoxY, this.craftBoxZ);
    }

    public boolean isCurrentRecipeBlockedByTier()
    {
        if (this.current_crafting_result == null || this.current_crafting_result.item_stack == null)
        {
            return false;
        }

        Material requiredMaterial;
        IRecipe recipe = this.getRecipe();

        if (recipe == null)
        {
            requiredMaterial = this.current_crafting_result.item_stack.getItem().getHardestMetalMaterial();
        }
        else
        {
            requiredMaterial = recipe.getMaterialToCheckToolBenchHardnessAgainst();
        }

        if (requiredMaterial == null)
        {
            return false;
        }

        int requiredRank = BlockCraftingBox.getToolMaterialRank(requiredMaterial);

        if (requiredRank < 0)
        {
            return false;
        }

        Material terminalMaterial = BlockCraftingBox.getToolMaterialForTier(this.getCraftingTier());
        int terminalRank = BlockCraftingBox.getToolMaterialRank(terminalMaterial);
        return requiredRank > terminalRank;
    }

    @Override
    public void onUpdate()
    {
        if (this.player instanceof EntityOtherPlayerMP)
        {
            return;
        }

        this.onCraftMatrixChanged(this.craftMatrix);
        SlotCrafting craftingSlot = this.getCraftingSlot();
        this.crafting_result_shown_but_prevented = this.isCurrentRecipeBlockedByTier();

        if (craftingSlot.checkCraftingResultIndex(this.player))
        {
            this.player.clearCrafting();
        }

        if (this.player instanceof EntityClientPlayerMP)
        {
            this.player.getAsEntityClientPlayerMP().crafting_experience_cost_tentative = 0;
        }

        if (!craftingSlot.canPlayerCraftItem(this.player))
        {
            if (this.player instanceof EntityClientPlayerMP && this.crafting_result_shown_but_prevented)
            {
                this.player.getAsEntityClientPlayerMP().crafting_experience_cost_tentative =
                    this.player.getAsEntityClientPlayerMP().crafting_experience_cost;
            }
            this.player.clearCrafting();
        }

        if (this.player.worldObj.isRemote && this.player instanceof EntityClientPlayerMP)
        {
            ItemStack itemStack;
            EntityClientPlayerMP clientPlayer = (EntityClientPlayerMP)this.player;

            if (this.resumeProgressArmed && clientPlayer.crafting_proceed)
            {
                clientPlayer.crafting_ticks = Math.max(clientPlayer.crafting_ticks, this.resumeTicks);
                clientPlayer.crafting_period = Math.max(clientPlayer.crafting_period, this.resumePeriod);
                this.resumeProgressArmed = false;
                this.resumeTicks = -1;
                this.resumePeriod = -1;
            }

            if (clientPlayer.crafting_proceed
                && clientPlayer.hasFoodEnergy()
                && (itemStack = craftingSlot.getStack()) != null
                && itemStack.getItem().hasCraftingEffect()
                && clientPlayer.ticksExisted % 5 == 0
                && clientPlayer.worldObj.rand.nextInt(5) == 0)
            {
                clientPlayer.sendPacket(new Packet90BroadcastToAssociatedPlayers(
                    new Packet85SimpleSignal(EnumSignal.entity_fx, EnumEntityFX.crafting)
                        .setEntityID(clientPlayer.entityId)
                        .setShort(itemStack.itemID),
                    false));
            }

            if (clientPlayer.crafting_proceed
                && clientPlayer.hasFoodEnergy()
                && ++clientPlayer.crafting_ticks >= clientPlayer.crafting_period)
            {
                itemStack = craftingSlot.getStack();

                if (itemStack != null)
                {
                    int craftingExperienceCost = clientPlayer.crafting_experience_cost;
                    this.recordSlotStackSizes();
                    craftingSlot.onPickupFromSlot(clientPlayer, itemStack);
                    this.lockSlotsThatChanged();
                    Minecraft.theMinecraft.thePlayer.sendQueue.addToSendQueue(
                        new Packet85SimpleSignal(EnumSignal.crafting_completed).setInteger(craftingExperienceCost));

                    if (craftingExperienceCost > 0)
                    {
                        clientPlayer.crafting_proceed = false;
                    }
                    clientPlayer.crafting_ticks = 0;
                }
            }
        }

        super.onUpdate();
    }

    // Shift clicking
    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index)
    {
        if (playerIn.worldObj.isRemote)
        {
            return null;
        }

        Slot slotObject = (Slot) inventorySlots.get(index);

        if (slotObject != null && slotObject.getHasStack())
        {
            if (slotObject instanceof SlotCrafting)
            {
                boolean hasChanges = false;
                ItemStack[][] recipe = new ItemStack[9][];

                for (int i = 0; i < 9; i++)
                {
                    recipe[i] = new ItemStack[] { this.craftMatrix.getStackInSlot(i) };
                }

                ItemStack slotStack = slotObject.getStack();
                ItemStack resultStack = null;
                ItemStack original = slotStack.copy();
                int crafted = 0;

                while (true)
                {
                    if (!slotObject.getHasStack() || !ItemStack.areItemStacksEqual(slotObject.getStack(), slotStack, true))
                    {
                        break;
                    }

                    slotStack = slotObject.getStack();

                    if (crafted + slotStack.stackSize > slotStack.getMaxStackSize())
                    {
                        break;
                    }

                    resultStack = slotStack.copy();
                    boolean merged = this.mergeItemStack(slotStack, this.rowCount() * 9, this.rowCount() * 9 + 36, true);

                    if (!merged)
                    {
                        // Player inventory is full: still complete this craft.
                        // SlotCrafting output routing handles storage/drop fallback.
                        crafted += resultStack.stackSize;
                        slotObject.onSlotChange(slotStack, resultStack);
                        slotObject.onPickupFromSlot(playerIn, slotStack);

                        if (EZConfiguration.guiAutoRefill.getBooleanValue()
                            && (slotObject.getStack() == null || !ItemStack.areItemStacksEqual(original, slotObject.getStack(), true)))
                        {
                            if (tryToPopulateCraftingGrid(recipe, playerIn, false))
                            {
                                hasChanges = true;
                            }
                        }
                        continue;
                    }

                    // It merged! grab another
                    crafted += resultStack.stackSize;
                    slotObject.onSlotChange(slotStack, resultStack);
                    slotObject.onPickupFromSlot(playerIn, slotStack);

                    if (EZConfiguration.guiAutoRefill.getBooleanValue()
                        && (slotObject.getStack() == null || !ItemStack.areItemStacksEqual(original, slotObject.getStack(), true)))
                    {
                        if (tryToPopulateCraftingGrid(recipe, playerIn, false))
                        {
                            hasChanges = true;
                        }
                    }
                }

                if (crafted > 0)
                {
                    saveGrid();
                    EZInventoryManager.sendToClients(inventory);
                    this.detectAndSendChanges();

                    if (playerIn instanceof ServerPlayer serverPlayer)
                    {
                        serverPlayer.sendContainerAndContentsToPlayer(this, this.getInventory());
                    }
                }

                if (resultStack == null || slotStack.stackSize == resultStack.stackSize)
                {
                    return null;
                }

                return resultStack;

            }
            else
            {
                ItemStack stackInSlot = slotObject.getStack();
                ItemStack movedStack = stackInSlot.copy();
                ItemStack remainingStack = this.inventory.input(stackInSlot);

                slotObject.putStack(remainingStack);
                slotObject.onSlotChanged();

                saveGrid();
                EZInventoryManager.sendToClients(inventory);
                this.detectAndSendChanges();

                if (playerIn instanceof ServerPlayer serverPlayer)
                {
                    serverPlayer.sendContainerAndContentsToPlayer(this, this.getInventory());
                }

                if (remainingStack == null || remainingStack.stackSize != movedStack.stackSize)
                {
                    return movedStack;
                }
            }
        }

        return null;
    }

    @Override
    public ItemStack customSlotClick(int slotId, int clickedButton, int mode, EntityPlayer playerIn)
    {
        if (playerIn.worldObj.isRemote)
        {
            return null;
        }

        if (slotId > 0 && mode == 0 && clickedButton == 0)
        {
            if (inventorySlots.size() > slotId)
            {
                Slot slotObject = (Slot) inventorySlots.get(slotId);

                if (slotObject != null)
                {
                    if (slotObject instanceof SlotCrafting)
                    {
                        ItemStack[][] recipe = new ItemStack[9][];

                        for (int i = 0; i < 9; i++)
                        {
                            recipe[i] = new ItemStack[] { this.craftMatrix.getStackInSlot(i) };
                        }
                        ItemStack result = super.customSlotClick(slotId, clickedButton, mode, playerIn);

                        if (result != null
                            && EZConfiguration.guiAutoRefill.getBooleanValue()
                            && tryToPopulateCraftingGrid(recipe, playerIn, false))
                        {
                            saveGrid();
                            EZInventoryManager.sendToClients(inventory);
                            this.detectAndSendChanges();

                            if (playerIn instanceof ServerPlayer serverPlayer)
                            {
                                serverPlayer.sendContainerAndContentsToPlayer(this, this.getInventory());
                            }
                        }
                        else if (result != null)
                        {
                            saveGrid();
                            EZInventoryManager.sendToClients(inventory);
                            this.detectAndSendChanges();

                            if (playerIn instanceof ServerPlayer serverPlayer)
                            {
                                serverPlayer.sendContainerAndContentsToPlayer(this, this.getInventory());
                            }
                        }

                        return result;
                    }
                }
            }

        }


        return super.customSlotClick(slotId, clickedButton, mode, playerIn);
    }

    public boolean tryToPopulateCraftingGrid(ItemStack[][] recipe, EntityPlayer playerIn, boolean usePlayerInv)
    {
        boolean hasChanges = false;
        // Maps playerInv slot index -> list of crafting grid slots that need an item from that player slot
        HashMap<Integer, ArrayList<Slot>> playerInvSlotsMapping = new HashMap<>();
        final int craftingSlotsStartIndex = inventorySlots.size() - 3 * 3;

        for (int j = 0; j < recipe.length; j++)
        {
            ItemStack[] recipeItems = recipe[j];

            Slot slot = getSlotFromInventory(this.craftMatrix, j);
            if (slot == null)
            {
                continue;
            }

            ItemStack stackInSlot = slot.getStack();

            if (stackInSlot != null)
            {
                if (getMatchingItemStackForRecipe(recipeItems, stackInSlot) != null)
                {
                    // Already has a valid item — force GUI update
                    inventoryItemStacks.set(craftingSlotsStartIndex + j, null);
                    continue;
                }
                // Return wrong item to storage
                ItemStack result = this.inventory.input(stackInSlot);

                if (result != null)
                {
                    playerIn.dropPlayerItemWithRandomChoice(result, false);
                }
                slot.putStack(null);
                hasChanges = true;
            }

            if (recipeItems == null || recipeItems.length == 0)
            {
                slot.putStack(null);
                continue;
            }

            // --- Try to find the item ---
            ItemStack retrieved = null;
            boolean foundInPlayerInv = false;

            for (int k = 0; k < recipeItems.length; k++)
            {
                ItemStack recipeItem = recipeItems[k];

                if (recipeItem == null) continue;

                // Normalize to 1 item
                ItemStack recipeItemOne = recipeItem.copy();
                recipeItemOne.stackSize = 1;

                // 1) Try storage first
                retrieved = getMatchingItemFromStorage(recipeItemOne);
                if (retrieved != null)
                {
                    hasChanges = true;
                    break;
                }

                // 2) Try player inventory if allowed
                if (usePlayerInv)
                {
                    Integer playerInvSize = playerIn.inventory.mainInventory.length;
                    for (int i = 0; i < playerInvSize; i++)
                    {
                        ItemStack playerItem = playerIn.inventory.mainInventory[i];

                        if (playerItem != null && isRecipeItemValid(recipeItemOne, playerItem))
                        {
                            ArrayList<Slot> targetSlots = playerInvSlotsMapping.get(i);
                            if (targetSlots == null)
                            {
                                targetSlots = new ArrayList<>();
                                playerInvSlotsMapping.put(i, targetSlots);
                            }
                            // Check we haven't already consumed all copies of this item
                            if (playerItem.stackSize > targetSlots.size())
                            {
                                targetSlots.add(slot);
                                foundInPlayerInv = true;
                                break;
                            }
                        }
                    }

                    if (foundInPlayerInv) break;
                }
            }

            if (retrieved != null)
            {
                slot.putStack(retrieved);
            }
            else if (!foundInPlayerInv)
            {
                // Nothing found anywhere — clear slot
                slot.putStack(null);
            }
            // If foundInPlayerInv==true, the second loop below will fill the slot
        }

        // Second pass: transfer items from player inventory into crafting grid slots
        if (usePlayerInv && !playerInvSlotsMapping.isEmpty())
        {
            Set<Entry<Integer, ArrayList<Slot>>> set = playerInvSlotsMapping.entrySet();

            for (Entry<Integer, ArrayList<Slot>> entry : set)
            {
                Integer playerInvSlotId = entry.getKey();
                ArrayList<Slot> targetSlots = entry.getValue();
                int targetSlotsCount = targetSlots.size();

                ItemStack playerInvSlot = playerIn.inventory.mainInventory[playerInvSlotId];
                if (playerInvSlot == null) continue;

                // Distribute evenly, last slot gets the remainder
                int itemsToRequest = playerInvSlot.stackSize / targetSlotsCount;
                if (itemsToRequest < 1) itemsToRequest = 1;

                for (int j = 0; j < targetSlotsCount; j++)
                {
                    Slot targetSlot = targetSlots.get(j);
                    if (targetSlot == null) continue;

                    // Re-fetch in case previous iteration consumed some
                    playerInvSlot = playerIn.inventory.mainInventory[playerInvSlotId];
                    if (playerInvSlot == null) break;

                    int toTake = (j == targetSlotsCount - 1) ? playerInvSlot.stackSize : itemsToRequest;
                    if (toTake < 1) toTake = 1;

                    ItemStack taken = playerIn.inventory.decrStackSize(playerInvSlotId, toTake);
                    if (taken != null && taken.stackSize > 0)
                    {
                        // If slot already has a compatible item (e.g. from a previous grid state), merge
                        ItemStack existing = targetSlot.getStack();
                        if (existing != null && EZInventory.stacksEqual(existing, taken))
                        {
                            existing.stackSize += taken.stackSize;
                        }
                        else
                        {
                            targetSlot.putStack(taken);
                        }
                        hasChanges = true;
                    }
                    else
                    {
                        if (targetSlot.getStack() == null)
                        {
                            targetSlot.putStack(null);
                        }
                    }
                }
            }
        }

        return hasChanges;
    }

    private ItemStack getMatchingItemFromStorage(ItemStack recipeItem)
    {
        for (int i = 0; i < this.inventory.inventory.size(); i++)
        {
            ItemStack group = this.inventory.inventory.get(i);

            if (isRecipeItemValid(recipeItem, group))
            {
                if (group.stackSize >= recipeItem.stackSize)
                {
                    ItemStack stack = group.copy();
                    stack.stackSize = recipeItem.stackSize;
                    group.stackSize -= recipeItem.stackSize;

                    if (group.stackSize <= 0)
                    {
                        this.inventory.inventory.remove(i);
                    }
                    this.inventory.setHasChanges();
                    return stack;
                }
            }
        }

        return null;
    }

    private static boolean isRecipeItemValid(ItemStack recipeItem, ItemStack candidate)
    {
        if (recipeItem == null || candidate == null || recipeItem.getItem() == null || candidate.getItem() == null)
            return false;
        // Check item ID match
        if (recipeItem.itemID == candidate.itemID)
        {
            // Wildcard damage (32767) or exact damage match or damageable
            if (recipeItem.getItemDamage() == Short.MAX_VALUE
                || recipeItem.getItemDamage() == candidate.getItemDamage()
                || recipeItem.isItemStackDamageable()) {
                return true;
            }
        }

        return EZInventory.stacksEqual(recipeItem, candidate);
    }

    private static ItemStack getMatchingItemStackForRecipe(ItemStack[] recipeItems, ItemStack stack)
    {
        if (recipeItems == null)
        {
            return null;
        }
        for (ItemStack recipeItem : recipeItems)
        {
            if (isRecipeItemValid(recipeItem, stack))
            {
                return recipeItem;
            }
        }

        return null;
    }

    @Override
    protected int playerInventoryY() {
        return 174;
    }

    @Override
    protected int rowCount() {
        return 5;
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        captureCraftingProgressSnapshotOnClient();
        saveGrid();
        super.onContainerClosed(playerIn);
    }

    public void saveGrid() {
        if (this.inventory != null)
        {
            if (this.inventory.craftMatrix == null)
            {
                this.inventory.craftMatrix = new ItemStack[9];
            }

            boolean hasChanges = false;

            for (int i = 0; i < 9; i++)
            {
                ItemStack current = this.craftMatrix.getStackInSlot(i);
                if (!areCraftGridStacksEqual(this.inventory.craftMatrix[i], current))
                {
                    hasChanges = true;
                }
                this.inventory.craftMatrix[i] = current == null ? null : current.copy();
            }
            if (hasChanges)
            {
                this.inventory.setHasChanges();
                EZInventoryManager.saveInventory(this.inventory);
            }
        }
    }

    private static boolean areCraftGridStacksEqual(ItemStack left, ItemStack right)
    {
        if (!EZInventory.stacksEqual(left, right))
        {
            return false;
        }

        if (left == null)
        {
            return true;
        }

        return right != null && left.stackSize == right.stackSize;
    }

    public void clearGrid(EntityPlayer playerIn)
    {
        boolean cleared = false;

        for (int i = 0; i < 9; i++)
        {
            ItemStack stack = this.craftMatrix.getStackInSlot(i);

            if (stack != null)
            {
                ItemStack result = this.inventory.input(stack);
                this.craftMatrix.setInventorySlotContents(i, null);

                if (result != null)
                {
                    playerIn.dropPlayerItemWithRandomChoice(result, false);
                }
                cleared = true;
            }
        }

        if (cleared && !playerIn.worldObj.isRemote)
        {
            saveGrid(); // persist cleared matrix to inventory before broadcast
            EZInventoryManager.sendToClients(inventory);
        }
    }
}

