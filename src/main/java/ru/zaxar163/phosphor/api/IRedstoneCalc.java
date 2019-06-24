package ru.zaxar163.phosphor.api;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IRedstoneCalc {
	IBlockState calculateCurrentChanges0(World worldIn, BlockPos pos1, BlockPos pos2, IBlockState state);
}
