package ru.zaxar163.phosphor.api;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;

public interface ILightingEngine {
	void processLightUpdates(boolean isTickEvent);

	void processLightUpdatesForType(EnumSkyBlock lightType, boolean isTickEvent);

	void scheduleLightUpdate(EnumSkyBlock lightType, BlockPos pos, boolean isTickEvent);
}
