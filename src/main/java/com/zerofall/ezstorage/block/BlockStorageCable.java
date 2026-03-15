package com.zerofall.ezstorage.block;

import net.minecraft.AxisAlignedBB;
import net.minecraft.AABBIntercept;
import net.minecraft.Block;
import net.minecraft.Entity;
import net.minecraft.IBlockAccess;
import net.minecraft.Material;
import net.minecraft.Raycast;
import net.minecraft.RaycastCollision;
import net.minecraft.Vec3;
import net.minecraft.World;

import java.util.ArrayList;
import java.util.List;

public class BlockStorageCable extends StorageMultiblock {

    public static final float CORE_MIN = 6.0F / 16.0F;
    public static final float CORE_MAX = 10.0F / 16.0F;

    public BlockStorageCable(int id) {
        super(id, "storage_cable", Material.wood);
    }

    private AxisAlignedBB getDynamicBounds(IBlockAccess world, int x, int y, int z) {
        float minX = CORE_MIN;
        float minY = CORE_MIN;
        float minZ = CORE_MIN;
        float maxX = CORE_MAX;
        float maxY = CORE_MAX;
        float maxZ = CORE_MAX;

        if (canConnectToBlock(world, x - 1, y, z)) {
            minX = 0.0F;
        }
        if (canConnectToBlock(world, x + 1, y, z)) {
            maxX = 1.0F;
        }
        if (canConnectToBlock(world, x, y - 1, z)) {
            minY = 0.0F;
        }
        if (canConnectToBlock(world, x, y + 1, z)) {
            maxY = 1.0F;
        }
        if (canConnectToBlock(world, x, y, z - 1)) {
            minZ = 0.0F;
        }
        if (canConnectToBlock(world, x, y, z + 1)) {
            maxZ = 1.0F;
        }

        return AxisAlignedBB.getAABBPool().getAABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public void setBlockBoundsBasedOnStateAndNeighbors(IBlockAccess world, int x, int y, int z) {
        AxisAlignedBB bounds = this.getDynamicBounds(world, x, y, z);
        this.setBlockBoundsForCurrentThread(bounds);
    }

    @Override
    public void setBlockBoundsForItemRender(int metadata) {
        this.setBlockBoundsForAllThreads(CORE_MIN, CORE_MIN, CORE_MIN, CORE_MAX, CORE_MAX, CORE_MAX);
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        AxisAlignedBB bounds = this.getDynamicBounds(world, x, y, z);
        return AxisAlignedBB.getBoundingBoxFromPool(
                x, y, z,
                bounds.minX, bounds.minY, bounds.minZ,
                bounds.maxX, bounds.maxY, bounds.maxZ
        );
    }

    @Override
    public Object getRenderBounds(World world, int x, int y, int z, Entity entity) {
        AxisAlignedBB bounds = this.getDynamicBounds(world, x, y, z);
        return AxisAlignedBB.getBoundingBoxFromPool(
                x, y, z,
                bounds.minX, bounds.minY, bounds.minZ,
                bounds.maxX, bounds.maxY, bounds.maxZ
        );
    }

    @Override
    public Object getCollisionBounds(World world, int x, int y, int z, Entity entity) {
        // MITE expects local (0..1) AABBs when returning AxisAlignedBB[].
        List<AxisAlignedBB> boxes = new ArrayList<>(7);
        boxes.add(AxisAlignedBB.getBoundingBoxFromPool(CORE_MIN, CORE_MIN, CORE_MIN, CORE_MAX, CORE_MAX, CORE_MAX));

        if (canConnectToBlock(world, x, y - 1, z)) {
            boxes.add(AxisAlignedBB.getBoundingBoxFromPool(CORE_MIN, 0.0F, CORE_MIN, CORE_MAX, CORE_MIN, CORE_MAX));
        }
        if (canConnectToBlock(world, x, y + 1, z)) {
            boxes.add(AxisAlignedBB.getBoundingBoxFromPool(CORE_MIN, CORE_MAX, CORE_MIN, CORE_MAX, 1.0F, CORE_MAX));
        }
        if (canConnectToBlock(world, x, y, z - 1)) {
            boxes.add(AxisAlignedBB.getBoundingBoxFromPool(CORE_MIN, CORE_MIN, 0.0F, CORE_MAX, CORE_MAX, CORE_MIN));
        }
        if (canConnectToBlock(world, x, y, z + 1)) {
            boxes.add(AxisAlignedBB.getBoundingBoxFromPool(CORE_MIN, CORE_MIN, CORE_MAX, CORE_MAX, CORE_MAX, 1.0F));
        }
        if (canConnectToBlock(world, x - 1, y, z)) {
            boxes.add(AxisAlignedBB.getBoundingBoxFromPool(0.0F, CORE_MIN, CORE_MIN, CORE_MIN, CORE_MAX, CORE_MAX));
        }
        if (canConnectToBlock(world, x + 1, y, z)) {
            boxes.add(AxisAlignedBB.getBoundingBoxFromPool(CORE_MAX, CORE_MIN, CORE_MIN, 1.0F, CORE_MAX, CORE_MAX));
        }

        return boxes.toArray(new AxisAlignedBB[0]);
    }

    @Override
    public boolean isStandardFormCube(boolean[] is_standard_form_cube, int metadata) {
        return false;
    }

    @Override
    public boolean canBePlacedOnBlock(int metadata, Block block_below, int metadata_below, double y_offset)
    {
        if (block_below instanceof StorageMultiblock)
        {
            return true;
        }

        return super.canBePlacedOnBlock(metadata, block_below, metadata_below, y_offset);
    }

    @Override
    public RaycastCollision tryRaycastVsBlock(Raycast raycast, int x, int y, int z, Vec3 start, Vec3 end)
    {
        RaycastCollision closest = traceSegment(raycast, x, y, z, start, end, CORE_MIN, CORE_MIN, CORE_MIN, CORE_MAX, CORE_MAX, CORE_MAX);

        if (canConnectToBlock(raycast.getWorld(), x, y - 1, z))
        {
            closest = pickCloser(start, closest, traceSegment(raycast, x, y, z, start, end, CORE_MIN, 0.0F, CORE_MIN, CORE_MAX, CORE_MIN, CORE_MAX));
        }

        if (canConnectToBlock(raycast.getWorld(), x, y + 1, z))
        {
            closest = pickCloser(start, closest, traceSegment(raycast, x, y, z, start, end, CORE_MIN, CORE_MAX, CORE_MIN, CORE_MAX, 1.0F, CORE_MAX));
        }

        if (canConnectToBlock(raycast.getWorld(), x, y, z - 1))
        {
            closest = pickCloser(start, closest, traceSegment(raycast, x, y, z, start, end, CORE_MIN, CORE_MIN, 0.0F, CORE_MAX, CORE_MAX, CORE_MIN));
        }

        if (canConnectToBlock(raycast.getWorld(), x, y, z + 1))
        {
            closest = pickCloser(start, closest, traceSegment(raycast, x, y, z, start, end, CORE_MIN, CORE_MIN, CORE_MAX, CORE_MAX, CORE_MAX, 1.0F));
        }

        if (canConnectToBlock(raycast.getWorld(), x - 1, y, z))
        {
            closest = pickCloser(start, closest, traceSegment(raycast, x, y, z, start, end, 0.0F, CORE_MIN, CORE_MIN, CORE_MIN, CORE_MAX, CORE_MAX));
        }

        if (canConnectToBlock(raycast.getWorld(), x + 1, y, z))
        {
            closest = pickCloser(start, closest, traceSegment(raycast, x, y, z, start, end, CORE_MAX, CORE_MIN, CORE_MIN, 1.0F, CORE_MAX, CORE_MAX));
        }

        return closest;
    }

    private RaycastCollision traceSegment(Raycast raycast, int x, int y, int z, Vec3 start, Vec3 end, float minX, float minY, float minZ, float maxX, float maxY, float maxZ)
    {
        AxisAlignedBB box = AxisAlignedBB.getAABBPool().getAABB(minX, minY, minZ, maxX, maxY, maxZ).offset(x, y, z);
        AABBIntercept hit = box.calculateIntercept(raycast.getWorld(), start, end);
        return hit == null ? null : new RaycastCollision(raycast, x, y, z, hit.face_hit, hit.position_hit);
    }

    private RaycastCollision pickCloser(Vec3 start, RaycastCollision first, RaycastCollision second)
    {
        if (first == null)
        {
            return second;
        }

        if (second == null)
        {
            return first;
        }

        double firstDistance = first.position_hit.squareDistanceTo(start.xCoord, start.yCoord, start.zCoord);
        double secondDistance = second.position_hit.squareDistanceTo(start.xCoord, start.yCoord, start.zCoord);
        return secondDistance < firstDistance ? second : first;
    }

    public static boolean canConnectToBlock(IBlockAccess world, int x, int y, int z)
    {
        int id = world.getBlockId(x, y, z);

        if (id > 0 && id < Block.blocksList.length)
        {
            Block byId = Block.blocksList[id];

            if (byId instanceof StorageMultiblock)
            {
                return true;
            }
        }

        Block neighbor = world.getBlock(x, y, z);
        return neighbor instanceof StorageMultiblock;
    }

}
