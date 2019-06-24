/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ru.zaxar163.phosphor.mixins.fixes.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.BlockObserver;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockRedstoneTorch;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

@Mixin(value = BlockRedstoneWire.class, priority = 1001)
public abstract class MixinBlockRedstoneWire$1$Vanilla extends Block {

    protected MixinBlockRedstoneWire$1$Vanilla(final Material materialIn) {
        super(materialIn);
    }

    /** Positions that need to be turned off **/
    private List<BlockPos> panda$turnOff = Lists.newArrayList();
    /** Positions that need to be checked to be turned on **/
    private List<BlockPos> panda$turnOn = Lists.newArrayList();
    /** Positions of wire that was updated already (Ordering determines update order and is therefore required!) **/
    private final Set<BlockPos> panda$updatedRedstoneWire = Sets.newLinkedHashSet();
     
    /** Ordered arrays of the facings; Needed for the update order.
     *  I went with a vertical-first order here, but vertical last would work to.
     *  However it should be avoided to update the vertical axis between the horizontal ones as this would cause unneeded directional behavior. **/
    private static final EnumFacing[] facingsHorizontal = {EnumFacing.WEST, EnumFacing.EAST, EnumFacing.NORTH, EnumFacing.SOUTH};
    private static final EnumFacing[] facingsVertical = {EnumFacing.DOWN, EnumFacing.UP};
    private static final EnumFacing[] facings = ArrayUtils.addAll(facingsVertical, facingsHorizontal);

    /** Offsets for all surrounding blocks that need to receive updates **/
    private static final Vec3i[] surroundingBlocksOffset;
    static {
        final Set<Vec3i> set = Sets.newLinkedHashSet();
        for (final EnumFacing facing : facings) {
            set.add(facing.getDirectionVec());
        }
        for (final EnumFacing facing1 : facings) {
            final Vec3i v1 = facing1.getDirectionVec();
            for (final EnumFacing facing2 : facings) {
                final Vec3i v2 = facing2.getDirectionVec();
                set.add(new Vec3i(v1.getX() + v2.getX(), v1.getY() + v2.getY(), v1.getZ() + v2.getZ()));
            }
        }
        set.remove(new Vec3i(0, 0, 0));
        surroundingBlocksOffset = set.toArray(new Vec3i[set.size()]);
    }

    @Shadow public boolean canProvidePower;
    @Shadow protected abstract int getMaxCurrentStrength(World worldIn, BlockPos pos, int strength);
    @Shadow protected abstract boolean isPowerSourceAt(IBlockAccess worldIn, BlockPos pos, EnumFacing side);

    @Inject(method = "updateSurroundingRedstone", at = @At("HEAD"), cancellable = true)
    private void onUpdateSurroundingRedstone(final World worldIn, final BlockPos pos, final IBlockState state, final CallbackInfoReturnable<IBlockState> cir) {
        if (!worldIn.isRemote) {
            this.updateSurroundingRedstone(worldIn, pos);
            cir.setReturnValue(state);
        }
    }

    @Inject(method = "calculateCurrentChanges", at = @At("HEAD"), cancellable = true)
    private void onCalculateCurrentChanges(
        final World worldIn, final BlockPos pos1, final BlockPos pos2, final IBlockState state, final CallbackInfoReturnable<IBlockState> cir) {
        if (!worldIn.isRemote) {
            this.calculateCurrentChanges(worldIn, pos1);
            cir.setReturnValue(state);
        }
    }

    /**
     * Recalculates all surrounding wires and causes all needed updates
     * 
     * @author panda
     * 
     * @param worldIn World
     * @param pos Position that needs updating
     */
    private void updateSurroundingRedstone(final World worldIn, final BlockPos pos) {
        // Recalculate the connected wires
        this.calculateCurrentChanges(worldIn, pos);

        // Set to collect all the updates, to only execute them once. Ordering required.
        final Set<BlockPos> blocksNeedingUpdate = Sets.newLinkedHashSet();

        // Add the needed updates
        for (final BlockPos posi : this.panda$updatedRedstoneWire) {
            this.addBlocksNeedingUpdate(worldIn, posi, blocksNeedingUpdate);
        }
        // Add all other updates to keep known behaviors
        // They are added in a backwards order because it preserves a commonly used behavior with the update order
        final Iterator<BlockPos> it = Lists.newLinkedList(this.panda$updatedRedstoneWire).descendingIterator();
        while (it.hasNext()) {
            this.addAllSurroundingBlocks(it.next(), blocksNeedingUpdate);
        }
        // Remove updates on the wires as they just were updated
        blocksNeedingUpdate.removeAll(this.panda$updatedRedstoneWire);
        /*
         * Avoid unnecessary updates on the just updated wires A huge scale test
         * showed about 40% more ticks per second It's probably less in normal
         * usage but likely still worth it
         */
        this.panda$updatedRedstoneWire.clear();

        // Execute updates
        for (final BlockPos posi : blocksNeedingUpdate) {
            worldIn.notifyNeighborsOfStateChange(posi, (BlockRedstoneWire) (Object) this, false);
        }
    }

    /**
     * Turns on or off all connected wires
     * 
     * @param worldIn World
     * @param position Position of the wire that received the update
     */
    private void calculateCurrentChanges(final World worldIn, final BlockPos position) {
        // Turn off all connected wires first if needed
        if (worldIn.getBlockState(position).getBlock() == this) {
            this.panda$turnOff.add(position);
        } else {
            // In case this wire was removed, check the surrounding wires
            this.checkSurroundingWires(worldIn, position);
        }

        while (!this.panda$turnOff.isEmpty()) {
            final BlockPos pos = this.panda$turnOff.remove(0);
            final IBlockState state = worldIn.getBlockState(pos);
            final int oldPower = state.getValue(BlockRedstoneWire.POWER);
            this.canProvidePower = false;
            final int blockPower = worldIn.getRedstonePowerFromNeighbors(pos);
            this.canProvidePower = true;
            int wirePower = this.getSurroundingWirePower(worldIn, pos);
            // Lower the strength as it moved a block
            wirePower--;
            final int newPower = Math.max(blockPower, wirePower);

            // Power lowered?
            if (newPower < oldPower) {
                // If it's still powered by a direct source (but weaker) mark for turn on
                if (blockPower > 0 && !this.panda$turnOn.contains(pos)) {
                    this.panda$turnOn.add(pos);
                }
                // Set all the way to off for now, because wires that were powered by this need to update first
                setWireState(worldIn, pos, state, 0);
            // Power rose?
            } else if (newPower > oldPower) {
                // Set new Power
                this.setWireState(worldIn, pos, state, newPower);
            }
            // Check if surrounding wires need to change based on the current/new state and add them to the lists
            this.checkSurroundingWires(worldIn, pos);
        }
        // Now all needed wires are turned off. Time to turn them on again if there is a power source.
        while (!this.panda$turnOn.isEmpty()) {
            final BlockPos pos = this.panda$turnOn.remove(0);
            final IBlockState state = worldIn.getBlockState(pos);
            final int oldPower = state.getValue(BlockRedstoneWire.POWER);
            this.canProvidePower = false;
            final int blockPower = worldIn.getRedstonePowerFromNeighbors(pos);
            this.canProvidePower = true;
            int wirePower = this.getSurroundingWirePower(worldIn, pos);
            // Lower the strength as it moved a block
            wirePower--;
            final int newPower = Math.max(blockPower, wirePower);

            if (newPower > oldPower) {
                setWireState(worldIn, pos, state, newPower);
            } else if (newPower < oldPower) {
                // Add warning
            }
            // Check if surrounding wires need to change based on the current/new state and add them to the lists
            this.checkSurroundingWires(worldIn, pos);
        }
        this.panda$turnOff.clear();
        this.panda$turnOn.clear();
    }

    /**
     * Checks if an wire needs to be marked for update depending on the power next to it
     * 
     * @author panda
     * 
     * @param worldIn World
     * @param pos Position of the wire that might need to change
     * @param otherPower Power of the wire next to it
     */
    private void addWireToList(final World worldIn, final BlockPos pos, final int otherPower) {
        final IBlockState state = worldIn.getBlockState(pos);
        if (state.getBlock() == this) {
            final int power = state.getValue(BlockRedstoneWire.POWER);
            // Could get powered stronger by the neighbor?
            if (power < (otherPower - 1) && !this.panda$turnOn.contains(pos)) {
                // Mark for turn on check.
                this.panda$turnOn.add(pos);
            }
            // Should have powered the neighbor? Probably was powered by it and is in turn off phase.
            if (power > otherPower && !this.panda$turnOff.contains(pos)) {
                // Mark for turn off check.
                this.panda$turnOff.add(pos);
            }
        }
    }

    /**
     * Checks if the wires around need to get updated depending on this wires state.
     * Checks all wires below before the same layer before on top to keep
     * some more rotational symmetry around the y-axis. 
     * 
     * @author panda
     * 
     * @param worldIn World
     * @param pos Position of the wire
     */
    private void checkSurroundingWires(final World worldIn, final BlockPos pos) {
        final IBlockState state = worldIn.getBlockState(pos);
        int ownPower = 0;
        if (state.getBlock() == this) {
            ownPower = state.getValue(BlockRedstoneWire.POWER);
        }
        // Check wires on the same layer first as they appear closer to the wire
        for (final EnumFacing facing : facingsHorizontal) {
            final BlockPos offsetPos = pos.offset(facing);
            if (facing.getAxis().isHorizontal()) {
                this.addWireToList(worldIn, offsetPos, ownPower);
            }
        }
        for (final EnumFacing facingVertical : facingsVertical) {
            final BlockPos offsetPos = pos.offset(facingVertical);
            final boolean solidBlock = worldIn.getBlockState(offsetPos).isBlockNormalCube();
            for (final EnumFacing facingHorizontal : facingsHorizontal) {
                // wire can travel upwards if the block on top doesn't cut the wire (is non-solid)
                // it can travel down if the block below is solid and the block "diagonal" doesn't cut off the wire (is non-solid) 
                if ((facingVertical == EnumFacing.UP && !solidBlock) || (facingVertical == EnumFacing.DOWN && solidBlock && !worldIn.getBlockState(offsetPos.offset(facingHorizontal)).isBlockNormalCube())) {
                    this.addWireToList(worldIn, offsetPos.offset(facingHorizontal), ownPower);
                }
            }
        }
    }

    /**
     * Gets the maximum power of the surrounding wires
     * 
     * @author panda
     * 
     * @param worldIn World
     * @param pos Position of the asking wire
     * @return The maximum power of the wires that could power the wire at pos
     */
    private int getSurroundingWirePower(final World worldIn, final BlockPos pos) {
        int wirePower = 0;
        for (final EnumFacing enumfacing : EnumFacing.Plane.HORIZONTAL) {
            final BlockPos offsetPos = pos.offset(enumfacing);
            // Wires on the same layer
            wirePower = this.getMaxCurrentStrength(worldIn, offsetPos, wirePower);
            
            // Block below the wire need to be solid (Upwards diode of slabs/stairs/glowstone) and no block should cut the wire
            if(worldIn.getBlockState(offsetPos).isNormalCube() && !worldIn.getBlockState(pos.up()).isNormalCube()) {
                wirePower = this.getMaxCurrentStrength(worldIn, offsetPos.up(), wirePower);
                // Only get from power below if no block is cutting the wire
            } else if (!worldIn.getBlockState(offsetPos).isNormalCube()) {
                wirePower = this.getMaxCurrentStrength(worldIn, offsetPos.down(), wirePower);
            }
        }
        return wirePower;
    }

    /**
     * Adds all blocks that need to receive an update from a redstone change in this position.
     * This means only blocks that actually could change.
     * 
     * @author panda
     * 
     * @param worldIn World
     * @param pos Position of the wire
     * @param set Set to add the update positions too
     */
    private void addBlocksNeedingUpdate(final World worldIn, final BlockPos pos, final Set<BlockPos> set) {
        final List<EnumFacing> connectedSides = this.getSidesToPower(worldIn, pos);
        // Add the blocks next to the wire first (closest first order)
        for (final EnumFacing facing : facings) {
            final BlockPos offsetPos = pos.offset(facing);
            // canConnectTo() is not the nicest solution here as it returns true for e.g. the front of a repeater
            // canBlockBePowereFromSide catches these cases
            if (connectedSides.contains(facing.getOpposite()) || facing == EnumFacing.DOWN
                    || (facing.getAxis().isHorizontal() && canConnectToBlock(worldIn.getBlockState(offsetPos), facing, worldIn, pos))) {
                if (this.canBlockBePoweredFromSide(worldIn.getBlockState(offsetPos), facing, true))
                    set.add(offsetPos);
            }
        }
        // Later add blocks around the surrounding blocks that get powered
        for (final EnumFacing facing : facings) {
            final BlockPos offsetPos = pos.offset(facing);
            if (connectedSides.contains(facing.getOpposite()) || facing == EnumFacing.DOWN) {
                if (worldIn.getBlockState(offsetPos).isNormalCube()) {
                    for (final EnumFacing facing1 : facings) {
                        if (this.canBlockBePoweredFromSide(worldIn.getBlockState(offsetPos.offset(facing1)), facing1, false))
                            set.add(offsetPos.offset(facing1));
                    }
                }
            }
        }
    }

    /**
     * Checks if a block can get powered from a side.
     * This behavior would better be implemented per block type as follows:
     *  - return false as default. (blocks that are not affected by redstone don't need to be updated, it doesn't really hurt if they are either)
     *  - return true for all blocks that can get powered from all side and change based on it (doors, fence gates, trap doors, note blocks, lamps, dropper, hopper, TNT, rails, possibly more)
     *  - implement own logic for pistons, repeaters, comparators and redstone torches
     *  The current implementation was chosen to keep everything in one class.
     *  
     *  Why is this extra check needed?
     *  1. It makes sure that many old behaviors still work (QC + Pistons).
     *  2. It prevents updates from "jumping".
     *     Or rather it prevents this wire to update a block that would get powered by the next one of the same line.
     *     This is to prefer as it makes understanding the update order of the wire really easy. The signal "travels" from the power source.
     * 
     * @author panda
     * 
     * @param state      State of the block
     * @param side       Side from which it gets powered
     * @param isWire     True if it's powered by a wire directly, False if through a block
     * @return           True if the block can change based on the power level it gets on the given side, false otherwise
     */
    private boolean canBlockBePoweredFromSide(final IBlockState state, final EnumFacing side, final boolean isWire) {
        if (state.getBlock() instanceof BlockPistonBase && state.getValue(BlockPistonBase.FACING) == side.getOpposite()) {
            return false;
        }
        if (state.getBlock() instanceof BlockRedstoneDiode && state.getValue(BlockRedstoneDiode.FACING) != side.getOpposite()) {
            return isWire
                   && state.getBlock() instanceof BlockRedstoneComparator
                   && state.getValue(BlockRedstoneComparator.FACING).getAxis() != side.getAxis()
                   && side.getAxis().isHorizontal();
        }
        if (state.getBlock() instanceof BlockRedstoneTorch) {
            return !isWire && state.getValue(BlockRedstoneTorch.FACING) == side;
        }
        return true;
    }

    /**
     * Creates a list of all horizontal sides that can get powered by a wire.
     * The list is ordered the same as the facingsHorizontal.
     * 
     * @param worldIn World
     * @param pos Position of the wire
     * @return List of all facings that can get powered by this wire
     */
    private List<EnumFacing> getSidesToPower(final World worldIn, final BlockPos pos) {
        final List<EnumFacing> retval = Lists.newArrayList();
        for (final EnumFacing facing : facingsHorizontal) {
            if (isPowerSourceAt(worldIn, pos, facing))
                retval.add(facing);
        }
        if (retval.isEmpty())
            return Lists.newArrayList(facingsHorizontal);
        final boolean northsouth = retval.contains(EnumFacing.NORTH) || retval.contains(EnumFacing.SOUTH);
        final boolean eastwest = retval.contains(EnumFacing.EAST) || retval.contains(EnumFacing.WEST);
        if (northsouth) {
            retval.remove(EnumFacing.EAST);
            retval.remove(EnumFacing.WEST);
        }
        if (eastwest) {
            retval.remove(EnumFacing.NORTH);
            retval.remove(EnumFacing.SOUTH);
        }
        return retval;
    }

    /**
     * Adds all surrounding positions to a set.
     * This is the neighbor blocks, as well as their neighbors 
     * 
     * @param pos
     * @param set
     */
    private void addAllSurroundingBlocks(final BlockPos pos, final Set<BlockPos> set) {
        for (final Vec3i vect : surroundingBlocksOffset) {
            set.add(pos.add(vect));
        }
    }

    /**
     * Sets the block state of a wire with a new power level and marks for updates
     * 
     * @author panda
     * 
     * @param worldIn World
     * @param pos Position at which the state needs to be set
     * @param state Old state
     * @param power Power it should get set to
     */
    private void setWireState(final World worldIn, final BlockPos pos, IBlockState state, final int power) {
        state = state.withProperty(BlockRedstoneWire.POWER, power);
        worldIn.setBlockState(pos, state, 2);
        this.panda$updatedRedstoneWire.add(pos);
    }

    /**
     * @author panda
     * @reason Uses local surrounding block offset list for notifications.
     *
     * @param worldIn The world
     * @param pos The position
     * @param state The block state
     */
    @Override
    @Overwrite
    public void onBlockAdded(final World worldIn, final BlockPos pos, final IBlockState state) {
        if (!worldIn.isRemote) {
            this.updateSurroundingRedstone(worldIn, pos);
            for (final Vec3i vec : surroundingBlocksOffset) {
                worldIn.notifyNeighborsOfStateChange(pos.add(vec), this, false);
            }
        }
    }

    /**
     * @author panda
     * @reason Uses local surrounding block offset list for notifications.
     *
     * @param worldIn The world
     * @param pos The position
     */
    @Override
    @Overwrite
    public void breakBlock(final World worldIn, final BlockPos pos, final IBlockState state) {
        super.breakBlock(worldIn, pos, state);
        if (!worldIn.isRemote) {
            this.updateSurroundingRedstone(worldIn, pos);
            for (final Vec3i vec : surroundingBlocksOffset) {
                worldIn.notifyNeighborsOfStateChange(pos.add(vec), this, false);
            }
        }
    }

    /**
     * @author panda
     * @reason Changed to use getSidesToPower() to avoid duplicate implementation.
     *
     * @param blockState The block state
     * @param blockAccess The block access
     * @param pos The position
     * @param side The side
     */
    @SuppressWarnings("deprecation")
    @Override
    @Overwrite
    public int getWeakPower(final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos pos, final EnumFacing side) {
        if (!this.canProvidePower) {
            return 0;
        } else {
            if (side == EnumFacing.UP || this.getSidesToPower((World) blockAccess, pos).contains(side)) {
                return blockState.getValue(BlockRedstoneWire.POWER);
            } else {
                return 0;
            }
        }
    }

    // Forge adds 2 params to canConnectTo so we need to copy method in order to access it
    private static boolean canConnectToBlock(final IBlockState blockState, @Nullable final EnumFacing side, final IBlockAccess world, final BlockPos pos) {
        final Block block = blockState.getBlock();

        if (block == Blocks.REDSTONE_WIRE) {
            return true;
        }
        if (Blocks.UNPOWERED_REPEATER.isSameDiode(blockState)) {
            final EnumFacing enumfacing = blockState.getValue(BlockRedstoneRepeater.FACING);
            return enumfacing == side || enumfacing.getOpposite() == side;
        }
        if (Blocks.OBSERVER == blockState.getBlock()) {
            return side == blockState.getValue(BlockObserver.FACING);
        }
        return true;
    }
}