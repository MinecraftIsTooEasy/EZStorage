package com.zerofall.ezstorage.block;

import com.zerofall.ezstorage.compat.EZCraftingBoxCompat;
import com.zerofall.ezstorage.tileentity.TileEntityStorageCore;
import com.zerofall.ezstorage.util.BlockRef;
import com.zerofall.ezstorage.util.EZInventory;
import net.minecraft.*;

public class BlockCraftingBox extends StorageUserInterface {

    public static final int MAX_TIER = 7;

    public BlockCraftingBox(int id) {
        super(id, "crafting_box", Material.wood);
    }

    @Override
    public boolean isValidMetadata(int metadata) {
        return metadata >= 0 && metadata <= getMaxTier();
    }

    public static int getMaxTier() {
        return EZCraftingBoxCompat.getMaxTier();
    }

    @Override
    public int getBlockSubtypeUnchecked(int metadata) {
        return metadata;
    }

    @Override
    public int getItemSubtype(int metadata) {
        return metadata;
    }

    @Override
    public void breakBlock(World worldIn, int x, int y, int z, int blockId, int meta) {
        super.breakBlock(worldIn, x, y, z, blockId, meta);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, EnumFace face, float offset_x, float offset_y, float offset_z)
    {
        if (world.isRemote) return true;

        if (!(player instanceof ServerPlayer serverPlayer)) return true;

        TileEntityStorageCore core;
        TileEntity tileEntity = world.getBlockTileEntity(x, y, z);

        if (tileEntity instanceof TileEntityStorageCore)
        {
            core = (TileEntityStorageCore) tileEntity;
        }
        else
        {
            BlockRef blockRef = new BlockRef(this, x, y, z);
            core = findCore(blockRef, world, null);
        }

        if (core == null)
        {
            serverPlayer.sendChatToPlayer(ChatMessageComponent.createFromTranslationKey("chat.msg.storagecore_not_found"));
            return true;
        }

        EZInventory inventory = core.getInventory();

        if (inventory != null)
        {
            openPlayerInventoryGui(serverPlayer, inventory, world, x, y, z, core);
        }

        return true;
    }

    @Override
    public boolean isPortable(World world, EntityLivingBase entity_living_base, int x, int y, int z) {
        return true;
    }

    public static String getTierName(int tier)
    {
        String compatName = EZCraftingBoxCompat.getCustomTierName(tier);

        if (compatName != null) return compatName;

        return switch (tier)
        {
            case 1 -> StatCollector.translateToLocal("ezstorage.tier.copper");
            case 2 -> StatCollector.translateToLocal("ezstorage.tier.silver");
            case 3 -> StatCollector.translateToLocal("ezstorage.tier.gold");
            case 4 -> StatCollector.translateToLocal("ezstorage.tier.iron");
            case 5 -> StatCollector.translateToLocal("ezstorage.tier.ancient_metal");
            case 6 -> StatCollector.translateToLocal("ezstorage.tier.mithril");
            case 7 -> StatCollector.translateToLocal("ezstorage.tier.adamantium");
            default -> StatCollector.translateToLocal("ezstorage.tier.basic");
        };
    }

    public static int getTierGroup(int tier)
    {
        if (tier == 0) return 0;
        if (tier >= 1 && tier <= 3) return 1;
        if (tier == 4) return 2;
        if (tier == 5) return 3;
        if (tier == 6) return 4;

        // With ITFRB, adamantium is shifted behind tungsten.
        if (tier == 7) return EZCraftingBoxCompat.hasTungstenPath() ? 6 : 5;

        int compatGroup = EZCraftingBoxCompat.getTierGroupForCustomTier(tier);

        if (compatGroup > 0) return compatGroup;
        return 0;
    }

    public static int getMaxUpgradeGroup()
    {
        if (EZCraftingBoxCompat.hasInfinityTier()) return 8;
        if (EZCraftingBoxCompat.hasVibraniumTier()) return EZCraftingBoxCompat.hasTungstenPath() ? 7 : 6;
        if (EZCraftingBoxCompat.hasTungstenPath()) return 6;
        return 5;
    }

    /** Mirrors vanilla workbench progression: flint -> copper -> silver -> gold -> iron -> ancient -> mithril -> adamantium. */
    public static Material getToolMaterialForTier(int tier)
    {
        Material compatMaterial = EZCraftingBoxCompat.getCustomToolMaterialForTier(tier);

        if (compatMaterial != null) {
            return compatMaterial;
        }

        return switch (tier)
        {
            case 1 -> Material.copper;
            case 2 -> Material.silver;
            case 3 -> Material.gold;
            case 4 -> Material.iron;
            case 5 -> Material.ancient_metal;
            case 6 -> Material.mithril;
            case 7 -> Material.adamantium;
            default -> Material.flint;
        };
    }

    /** Vanilla-like ordering used for "needs better tools" checks. */
    public static int getToolMaterialRank(Material material)
    {
        int compatRank = EZCraftingBoxCompat.getCustomToolMaterialRank(material);

        if (compatRank > 0) {
            return compatRank;
        }

        if (material == null) return -1;
        if (material == Material.flint) return 0;
        if (material == Material.copper) return 1;
        if (material == Material.silver) return 2;
        if (material == Material.gold) return 3;
        if (material == Material.iron) return 4;
        if (material == Material.ancient_metal) return 5;
        if (material == Material.mithril) return 6;
        if (material == Material.adamantium) return EZCraftingBoxCompat.hasTungstenPath() ? 8 : 7;
        if (material == Material.obsidian) return EZCraftingBoxCompat.hasTungstenPath() ? 9 : 8;
        return -1;
    }

    /**
     * Returns required ingot group to upgrade from given group.
     */
    public static int getIngotGroup(Item held)
    {
        int compatGroup = EZCraftingBoxCompat.getCustomIngotGroup(held);

        if (compatGroup > 0) {
            return compatGroup;
        }

        if (held == Item.ingotCopper || held == Item.ingotSilver || held == Item.ingotGold) return 1;
        if (held == Item.ingotIron) return 2;
        if (held == Item.ingotAncientMetal) return 3;
        if (held == Item.ingotMithril) return 4;
        if (held == Item.ingotAdamantium) return EZCraftingBoxCompat.hasTungstenPath() ? 6 : 5;
        return -1;
    }

    /**
     * Returns the new tier metadata when upgrading with the given ingot.
     */
    public static int getTierForIngot(Item held)
    {
        int compatTier = EZCraftingBoxCompat.getCustomTierForIngot(held);

        if (compatTier > 0) {
            return compatTier;
        }

        if (held == Item.ingotCopper) return 1;
        if (held == Item.ingotSilver) return 2;
        if (held == Item.ingotGold) return 3;
        if (held == Item.ingotIron) return 4;
        if (held == Item.ingotAncientMetal) return 5;
        if (held == Item.ingotMithril) return 6;
        if (held == Item.ingotAdamantium) return 7;
        return -1;
    }

    /**
     * Returns the translation key for the required ingot group.
     */
    public static String getNeedGroupKey(int group)
    {
        if (group >= 6) {
            return "ezstorage.crafting_box.upgrade.need.default";
        }

        return switch (group)
        {
            case 1 -> "ezstorage.crafting_box.upgrade.need.group1";
            case 2 -> "ezstorage.crafting_box.upgrade.need.group2";
            case 3 -> "ezstorage.crafting_box.upgrade.need.group3";
            case 4 -> "ezstorage.crafting_box.upgrade.need.group4";
            case 5 -> "ezstorage.crafting_box.upgrade.need.group5";
            default -> "ezstorage.crafting_box.upgrade.need.default";
        };
    }

    public static String getNeedGroupText(int group)
    {
        if (group >= 6 || (group >= 5 && EZCraftingBoxCompat.hasTungstenPath())) {
            return EZCraftingBoxCompat.getNeedGroupText(group);
        }

        return StatCollector.translateToLocal(getNeedGroupKey(group));
    }
}