package com.zerofall.ezstorage.block;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.EntityLivingBase;
import net.minecraft.Explosion;
import net.minecraft.Material;
import net.minecraft.TileEntity;
import net.minecraft.World;

import com.zerofall.ezstorage.tileentity.TileEntityStorageCore;
import com.zerofall.ezstorage.util.BlockRef;
import com.zerofall.ezstorage.util.EZStorageUtils;

public class StorageMultiblock extends EZBlock {

    protected StorageMultiblock(int id, String name, Material material) {
        super(id, name, material);
    }

    @Override
    public void breakBlock(World worldIn, int x, int y, int z, int blockId, int meta) {
        super.breakBlock(worldIn, x, y, z, blockId, meta);
        attemptMultiblock(worldIn, x, y, z, null);
    }

    @Override
    public void onBlockDestroyedByExplosion(World worldIn, int x, int y, int z, Explosion explosionIn) {
        super.onBlockDestroyedByExplosion(worldIn, x, y, z, explosionIn);
        attemptMultiblock(worldIn, x, y, z, null);
    }

    @Override
    public void onBlockAdded(World worldIn, int x, int y, int z) {
        super.onBlockAdded(worldIn, x, y, z);
        attemptMultiblock(worldIn, x, y, z, null);
    }

    @Override
    public boolean canBePlacedAt(World worldIn, int x, int y, int z, int metadata)
    {
        if (!worldIn.isRemote)
        {
            Set<TileEntityStorageCore> coreSet = new HashSet<>();
            BlockRef br = new BlockRef(this, x, y, z);
            findMultipleCores(br, worldIn, null, coreSet);
            if (coreSet.size() > 1)
            {
                return false;
            }
        }

        return super.canBePlacedAt(worldIn, x, y, z, metadata);
    }

    /**
     * Attempt to form the multiblock structure by searching for the core, then telling the core to scan the multiblock
     * 
     * @param world
     * @param x
     * @param y
     * @param z
     */
    public void attemptMultiblock(World world, int x, int y, int z, EntityLivingBase entity)
    {
        if (!world.isRemote)
        {
            if (!(this instanceof BlockStorageCore))
            {
                BlockRef blockRef = new BlockRef(this, x, y, z);
                TileEntityStorageCore core = findCore(blockRef, world, null);

                if (core != null)
                {
                    core.scanMultiblock(entity);
                }
            }
        }
    }

    /**
     * Recursive function that searches for a StorageCore in a multiblock structure
     * 
     * @param br
     * @param world
     * @param scanned
     * @return
     */
    public TileEntityStorageCore findCore(BlockRef br, World world, Set<BlockRef> scanned)
    {
        if (scanned == null)
        {
            scanned = new HashSet<>();
        }
        List<BlockRef> neighbors = EZStorageUtils.getNeighbors(br.posX, br.posY, br.posZ, world);
        for (BlockRef blockRef : neighbors)
        {
            if (blockRef.block instanceof StorageMultiblock)
            {
                if (blockRef.block instanceof BlockStorageCore)
                {
                    TileEntity tileEntity = world.getBlockTileEntity(blockRef.posX, blockRef.posY, blockRef.posZ);
                    if (tileEntity instanceof TileEntityStorageCore)
                    {
                        return (TileEntityStorageCore) tileEntity;
                    }
                }
                else
                {
                    if (scanned.add(blockRef) == true)
                    {
                        TileEntityStorageCore entity = findCore(blockRef, world, scanned);
                        if (entity != null)
                        {
                            return entity;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Recursive function that searches for a StorageCore in a multiblock structure
     * 
     * @param br
     * @param world
     * @param scanned
     * @return
     */
    public void findMultipleCores(BlockRef br, World world, Set<BlockRef> scanned, Set<TileEntityStorageCore> cores)
    {
        if (scanned == null)
        {
            scanned = new HashSet<>();
        }
        List<BlockRef> neighbors = EZStorageUtils.getNeighbors(br.posX, br.posY, br.posZ, world);
        for (BlockRef blockRef : neighbors)
        {
            if (blockRef.block instanceof StorageMultiblock)
            {
                if (blockRef.block instanceof BlockStorageCore)
                {
                    TileEntity tileEntity = world.getBlockTileEntity(blockRef.posX, blockRef.posY, blockRef.posZ);
                    if (tileEntity instanceof TileEntityStorageCore)
                    {
                        cores.add((TileEntityStorageCore) tileEntity);
                    }
                }
                else
                {
                    if (scanned.add(blockRef) == true)
                    {
                        findMultipleCores(blockRef, world, scanned, cores);
                    }
                }
            }
        }
    }

}