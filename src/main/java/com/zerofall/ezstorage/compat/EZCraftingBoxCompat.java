package com.zerofall.ezstorage.compat;

import java.util.IdentityHashMap;
import net.minecraft.Item;
import net.minecraft.ItemStack;
import net.minecraft.Material;
import net.minecraft.StatCollector;
import moddedmite.rustedironcore.api.block.WorkbenchBlock;
import net.minecraft.Block;
import net.moddedmite.mitemod.bex.register.BEXItems;
import net.moddedmite.mitemod.bex.register.BEXMaterials;
import net.xiaoyu233.mitemod.miteite.item.MITEITEItemRegistryInit;
import net.xiaoyu233.fml.FishModLoader;
import net.xiaoyu233.mitemod.miteite.item.material.Materials;

/**
 * Internal compat bridge for optional mod materials/ingots used by crafting-box upgrades.
 */
public final class EZCraftingBoxCompat {

    public static final int BASE_MAX_TIER = 7;
    public static final int TIER_NICKEL = 8;
    public static final int TIER_TUNGSTEN = 9;
    public static final int TIER_VIBRANIUM = 10;
    public static final int TIER_INFINITY = 11;

    public static final boolean HAS_ITFRB = FishModLoader.hasMod("mite-itf-reborn");
    public static final boolean HAS_BEX = FishModLoader.hasMod("bex");
    public static final boolean HAS_ITE = FishModLoader.hasMod("mite_ite");
    public static final boolean HAS_EXTREME = FishModLoader.hasMod("extreme");

    private static boolean initialized;

    private static Item nickelIngot;
    private static Item tungstenIngot;
    private static Item vibraniumIngot;
    private static Item infinityIngot;

    private static Material nickelMaterial;
    private static Material tungstenMaterial;
    private static Material vibraniumMaterial;
    private static Material infinityMaterial;
    private static final IdentityHashMap<Material, Float> workbenchSpeedByMaterial = new IdentityHashMap<>();
    private static boolean workbenchSpeedsScanned;

    private EZCraftingBoxCompat() {
    }

    private static void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        if (HAS_ITFRB) {
            nickelIngot = net.oilcake.mitelros.registry.item.Items.nickelIngot;
            tungstenIngot = net.oilcake.mitelros.registry.item.Items.tungstenIngot;
            nickelMaterial = net.oilcake.mitelros.material.Materials.nickel;
            tungstenMaterial = net.oilcake.mitelros.material.Materials.tungsten;
        }

        if (HAS_ITE) {
            vibraniumIngot = MITEITEItemRegistryInit.VIBRANIUM_INGOT;
            vibraniumMaterial = Materials.vibranium;
        }

        if (HAS_BEX) {
            infinityIngot = BEXItems.infinityingot;
            infinityMaterial = BEXMaterials.infinity;
        }

        initialized = true;
    }

    public static int getMaxTier() {
        ensureInitialized();

        if (canUseInfinityTier()) {
            return TIER_INFINITY;
        }
        if (vibraniumIngot != null) {
            return TIER_VIBRANIUM;
        }
        if (tungstenIngot != null) {
            return TIER_TUNGSTEN;
        }
        if (nickelIngot != null) {
            return TIER_NICKEL;
        }
        return BASE_MAX_TIER;
    }

    public static int getCustomTierForIngot(Item held) {
        ensureInitialized();

        if (held == null) {
            return -1;
        }
        if (held == nickelIngot) {
            return TIER_NICKEL;
        }
        if (held == tungstenIngot) {
            return TIER_TUNGSTEN;
        }
        if (held == vibraniumIngot) {
            return TIER_VIBRANIUM;
        }
        if (held == infinityIngot && canUseInfinityTier()) {
            return TIER_INFINITY;
        }
        return -1;
    }

    public static int getCustomIngotGroup(Item held) {
        ensureInitialized();

        if (held == null) {
            return -1;
        }
        if (held == nickelIngot) {
            // ITFRB nickel is the iron-equivalent gate: copper/silver/gold -> nickel -> ancient.
            return 2;
        }
        if (held == tungstenIngot) {
            // Mithril must pass through tungsten before adamantium when ITFRB is present.
            return 5;
        }
        if (held == vibraniumIngot) {
            return hasTungstenPath() ? 7 : 6;
        }
        if (held == infinityIngot && canUseInfinityTier()) {
            return getInfinityGroup();
        }
        return -1;
    }

    public static int getTierGroupForCustomTier(int tier) {
        ensureInitialized();

        return switch (tier) {
            case TIER_NICKEL -> 2;
            case TIER_TUNGSTEN -> 5;
            case TIER_VIBRANIUM -> hasTungstenPath() ? 7 : 6;
            case TIER_INFINITY -> canUseInfinityTier() ? getInfinityGroup() : 0;
            default -> 0;
        };
    }

    public static Material getCustomToolMaterialForTier(int tier) {
        ensureInitialized();

        return switch (tier) {
            case TIER_NICKEL -> nickelMaterial;
            case TIER_TUNGSTEN -> tungstenMaterial;
            case TIER_VIBRANIUM -> vibraniumMaterial;
            case TIER_INFINITY -> infinityMaterial;
            default -> null;
        };
    }

    public static int getCustomToolMaterialRank(Material material) {
        ensureInitialized();

        if (material == null) {
            return -1;
        }
        if (material == nickelMaterial) {
            // nickel behaves like iron in progression checks
            return 4;
        }
        if (material == tungstenMaterial) {
            return 7;
        }
        if (material == vibraniumMaterial) {
            return hasTungstenPath() ? 9 : 8;
        }
        if (material == infinityMaterial) {
            return hasTungstenPath() ? 10 : 9;
        }
        return -1;
    }

    public static String getCustomTierName(int tier) {
        ensureInitialized();

        Item ingot = getIngotForTier(tier);

        if (ingot == null) {
            return null;
        }

        try {
            return stripIngotSuffix(new ItemStack(ingot, 1).getDisplayName());
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String getNeedGroupText(int group) {
        ensureInitialized();

        Item ingot = switch (group) {
            case 2 -> nickelIngot;
            case 5 -> tungstenIngot;
            case 6 -> hasTungstenPath() ? Item.ingotAdamantium : vibraniumIngot;
            case 7 -> hasTungstenPath() ? vibraniumIngot : (canUseInfinityTier() ? infinityIngot : null);
            case 8 -> canUseInfinityTier() ? infinityIngot : null;
            default -> null;
        };

        if (ingot == null) {
            return StatCollector.translateToLocal("ezstorage.crafting_box.upgrade.need.default");
        }

        try {
            return new ItemStack(ingot, 1).getDisplayName();
        } catch (Exception ignored) {
            return StatCollector.translateToLocal("ezstorage.crafting_box.upgrade.need.default");
        }
    }

    public static float getBenchAndToolsModifierForMaterial(Material material) {
        ensureInitialized();

        if (material == null) {
            return 0.2F;
        }

        float reducedProfileModifier = getReducedProfileModifier(material);

        if (reducedProfileModifier >= 0.0F) {
            return reducedProfileModifier;
        }

        float customModifier = getRustedIronWorkbenchSpeedModifier(material);

        if (customModifier >= 0.0F) {
            return applyVibraniumSpeed(material, customModifier);
        }

        if (material == Material.flint || material == Material.obsidian) {
            return 0.2F;
        }
        if (material == Material.copper || material == Material.silver || material == Material.gold) {
            return 0.3F;
        }
        if (material == Material.iron || material == nickelMaterial) {
            return 0.4F;
        }
        if (material == Material.ancient_metal) {
            return 0.5F;
        }
        if (material == Material.mithril) {
            return 0.6F;
        }
        if (material == Material.adamantium || material == tungstenMaterial) {
            return 0.7F;
        }
        if (material == vibraniumMaterial) {
            return applyVibraniumSpeed(material, hasTungstenPath() ? 0.8F : 0.7F);
        }
        if (material == infinityMaterial) {
            return hasTungstenPath() ? 0.9F : 0.8F;
        }

        return 0.2F;
    }

    /**
     * ITE/Extreme globally reduce workbench crafting boosts.
     * Keep EZStorage vibranium tier aligned with that profile.
     */
    private static float getReducedProfileModifier(Material material) {
        if (!hasReducedWorkbenchBoostProfile()) {
            return -1.0F;
        }

        if (material == Material.flint || material == Material.obsidian) {
            return 0.0F;
        }
        if (material == Material.copper || material == Material.silver || material == Material.gold) {
            return 0.1F;
        }
        if (material == Material.iron || material == nickelMaterial) {
            return 0.2F;
        }
        if (material == Material.ancient_metal || material == Material.mithril) {
            return 0.3F;
        }
        if (material == Material.adamantium || material == tungstenMaterial) {
            return 0.4F;
        }
        if (material == vibraniumMaterial) {
            return applyVibraniumSpeed(material, 0.55F);
        }

        return -1.0F;
    }

    private static float applyVibraniumSpeed(Material material, float modifier) {
        if (material == vibraniumMaterial && hasReducedWorkbenchBoostProfile()) {
            float adamantiumModifier = 0.4F;

            float corrected = modifier;

            if (corrected <= adamantiumModifier) {
                corrected = Math.min(adamantiumModifier + 0.05F, 0.95F);
            }

            return corrected;
        }
        return modifier;
    }

    private static boolean hasReducedWorkbenchBoostProfile() {
        return HAS_ITE || HAS_EXTREME;
    }

    public static Item getIngotForTier(int tier) {
        ensureInitialized();

        return switch (tier) {
            case TIER_NICKEL -> nickelIngot;
            case TIER_TUNGSTEN -> tungstenIngot;
            case TIER_VIBRANIUM -> vibraniumIngot;
            case TIER_INFINITY -> canUseInfinityTier() ? infinityIngot : null;
            default -> null;
        };
    }

    public static boolean hasNickelTier() {
        ensureInitialized();
        return nickelIngot != null;
    }

    public static boolean hasTungstenPath() {
        ensureInitialized();
        return tungstenIngot != null;
    }

    public static boolean hasVibraniumTier() {
        ensureInitialized();
        return vibraniumIngot != null;
    }

    public static boolean hasInfinityTier() {
        ensureInitialized();
        return canUseInfinityTier();
    }

    private static int getInfinityGroup() {
        return hasTungstenPath() ? 8 : 7;
    }

    private static float getRustedIronWorkbenchSpeedModifier(Material material) {
        if (!workbenchSpeedsScanned) {
            scanWorkbenchSpeeds();
        }

        Float speedModifier = workbenchSpeedByMaterial.get(material);
        return speedModifier == null ? -1.0F : speedModifier;
    }

    private static void scanWorkbenchSpeeds() {
        workbenchSpeedsScanned = true;
        workbenchSpeedByMaterial.clear();

        for (Block block : Block.blocksList) {
            if (!(block instanceof WorkbenchBlock workbenchBlock)) {
                continue;
            }

            Material material = workbenchBlock.getMaterial();

            if (material == null) {
                continue;
            }

            float speedModifier = workbenchBlock.getSpeedModifier();
            Float existing = workbenchSpeedByMaterial.get(material);

            if (existing == null || speedModifier > existing) {
                // Keep the fastest bench for each material.
                workbenchSpeedByMaterial.put(material, speedModifier);
            }
        }
    }

    private static boolean canUseInfinityTier() {
        return infinityIngot != null && vibraniumIngot != null;
    }

    private static String stripIngotSuffix(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        String trimmed = name.trim();

        if (trimmed.endsWith("锭")) {
            return trimmed.substring(0, trimmed.length() - 1).trim();
        }

        if (trimmed.endsWith("之")) {
            return trimmed.substring(0, trimmed.length() - 2).trim();
        }


        String lower = trimmed.toLowerCase();

        if (lower.endsWith(" ingot")) {
            return trimmed.substring(0, trimmed.length() - 6).trim();
        }

        if (lower.endsWith("ingot")) {
            return trimmed.substring(0, trimmed.length() - 5).trim();
        }

        return trimmed;
    }

}
