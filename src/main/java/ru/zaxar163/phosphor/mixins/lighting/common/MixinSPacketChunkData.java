package ru.zaxar163.phosphor.mixins.lighting.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.world.chunk.Chunk;
import ru.zaxar163.phosphor.api.ILightingEngineProvider;

@Mixin(SPacketChunkData.class)
public abstract class MixinSPacketChunkData {
	/**
	 * @author Angeline Injects a callback into
	 *         SPacketChunkData#calculateChunkSize(Chunk, booolean, int) to force
	 *         light updates to be processed before creating the client payload. We
	 *         use this method rather than the constructor as it is not valid to
	 *         inject elsewhere other than the RETURN of a ctor, which is too late
	 *         for our needs.
	 */
	@Inject(method = "calculateChunkSize", at = @At("HEAD"))
	private void onCalculateChunkSize(Chunk chunkIn, boolean hasSkyLight, int changedSectionFilter,
			CallbackInfoReturnable<Integer> cir) {
		((ILightingEngineProvider) chunkIn).getLightingEngine().processLightUpdates(true);
	}
}
