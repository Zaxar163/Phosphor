package ru.zaxar163.phosphor.mixins.fixes.server;

import java.io.File;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.ServerEula;

@Mixin(ServerEula.class)
public class MixinServerEula {
	@Inject(method = "loadEULAFile", at = @At("RETURN"), cancellable = true)
	public void loadEULAFileHook(final File unused, final CallbackInfoReturnable<Boolean> cir) {
		if (Boolean.getBoolean("minecraft.eula.accepted")) {
			cir.setReturnValue(true);
			cir.cancel();
		}
	}
}
