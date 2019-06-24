package ru.zaxar163.phosphor.mixins.fixes.common;

import net.minecraft.server.MinecraftServer;
import ru.zaxar163.optim.Threader;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
    /**
     * @reason Disable initial world chunk load. This makes world load much faster, but in exchange
     * the player may see incomplete chunks (like when teleporting to a new area).
     */
    @Overwrite
    public void initialWorldChunkLoad() {}

    @Redirect(method = "startServerThread", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;start()V", remap = false))
    private void onServerStart(Thread thread) throws IllegalAccessException {
        Threader.SERVER.set(thread);
        thread.start();

    }

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;systemExitNow()V"))
    private void onServerStop(CallbackInfo ci) throws IllegalAccessException {
    	Threader.SERVER.set(null);
    }
}
