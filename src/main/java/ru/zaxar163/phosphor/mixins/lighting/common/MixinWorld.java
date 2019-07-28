package ru.zaxar163.phosphor.mixins.lighting.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import ru.zaxar163.phosphor.api.ILightingEngineProvider;
import ru.zaxar163.phosphor.world.lighting.LightingEngine;

@Mixin(World.class)
public abstract class MixinWorld implements ILightingEngineProvider {
	private LightingEngine lightingEngine;

	/**
	 * Directs the light update to the lighting engine and always returns a success
	 * value.
	 * 
	 * @author Angeline
	 */
	@Inject(method = "checkLightFor", at = @At("HEAD"), cancellable = true)
	private void checkLightFor(EnumSkyBlock type, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		lightingEngine.scheduleLightUpdate(type, pos, false);

		cir.setReturnValue(true);
	}

	@Override
	public LightingEngine getLightingEngine() {
		return lightingEngine;
	}

	/**
	 * @author Angeline Initialize the lighting engine on world construction.
	 */
	@Inject(method = "<init>", at = @At("RETURN"))
	private void onConstructed(CallbackInfo ci) {
		lightingEngine = new LightingEngine((World) (Object) this);
	}
}
