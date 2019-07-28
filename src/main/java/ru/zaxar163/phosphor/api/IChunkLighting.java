package ru.zaxar163.phosphor.api;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;

public interface IChunkLighting {
	int getCachedLightFor(EnumSkyBlock enumSkyBlock, BlockPos pos);
}
