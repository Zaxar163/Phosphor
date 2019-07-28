package ru.zaxar163.phosphor.mixins.lighting.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

@Mixin(ExtendedBlockStorage.class)
public class MixinExtendedBlockStorage {
	@Shadow
	private NibbleArray skyLight;

	@Shadow
	private int blockRefCount;

	@Shadow
	private NibbleArray blockLight;

	private int lightRefCount = -1;

	private boolean checkLightArrayEqual(NibbleArray storage, byte val) {
		if (storage == null)
			return true;

		byte[] arr = storage.getData();

		for (byte b : arr)
			if (b != val)
				return false;

		return true;
	}

	/**
	 * @author Angeline
	 * @reason Send light data to clients when lighting is non-trivial
	 */
	@Overwrite
	public boolean isEmpty() {
		if (blockRefCount != 0)
			return false;

		// -1 indicates the lightRefCount needs to be re-calculated
		if (lightRefCount == -1)
			if (checkLightArrayEqual(skyLight, (byte) 0xFF) && checkLightArrayEqual(blockLight, (byte) 0x00))
				lightRefCount = 0; // Lighting is trivial, don't send to clients
			else
				lightRefCount = 1; // Lighting is not trivial, send to clients

		return lightRefCount == 0;
	}

	/**
	 * @author Angeline
	 * @author Reset lightRefCount on call
	 */
	@Overwrite
	public void setBlockLight(int x, int y, int z, int value) {
		blockLight.set(x, y, z, value);
		lightRefCount = -1;
	}

	/**
	 * @author Angeline
	 * @author Reset lightRefCount on call
	 */
	@Overwrite
	public void setBlockLight(NibbleArray array) {
		blockLight = array;
		lightRefCount = -1;
	}

	/**
	 * @author Angeline
	 * @author Reset lightRefCount on call
	 */
	@Overwrite
	public void setSkyLight(int x, int y, int z, int value) {
		skyLight.set(x, y, z, value);
		lightRefCount = -1;
	}

	/**
	 * @author Angeline
	 * @reason Reset lightRefCount on call
	 */
	@Overwrite
	public void setSkyLight(NibbleArray array) {
		skyLight = array;
		lightRefCount = -1;
	}
}
