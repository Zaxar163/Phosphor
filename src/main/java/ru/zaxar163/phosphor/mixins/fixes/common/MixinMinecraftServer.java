package ru.zaxar163.phosphor.mixins.fixes.common;

import net.minecraft.crash.CrashReport;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ReportedException;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;
import ru.zaxar163.phosphor.PhosphorData;

import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
	@Shadow @Final private static Logger LOGGER;
	@Shadow public WorldServer[] worlds;
    /**
     * @reason Disable initial world chunk load. This makes world load much faster, but in exchange
     * the player may see incomplete chunks (like when teleporting to a new area).
     */
    @Overwrite
    public void initialWorldChunkLoad() {}

    @Redirect(method = "startServerThread", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;start()V", remap = false))
    private void onServerStart(Thread thread) throws IllegalAccessException {
        PhosphorData.SERVER.set(thread);
        thread.start();

    }

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;systemExitNow()V"))
    private void onServerStop(CallbackInfo ci) throws IllegalAccessException {
    	PhosphorData.SERVER.set(null);
    }

    @Overwrite
    public void saveAllWorlds(boolean isSilent)
    {
        for (WorldServer worldserver : this.worlds)
        {
            if (worldserver != null)
            {
                if (!isSilent)
                {
                    LOGGER.info("Saving chunks for level '{}'/{}", worldserver.getWorldInfo().getWorldName(), worldserver.provider.getDimensionType().getName());
                }

                try
                {
                    worldserver.saveAllChunks(true, PhosphorData.PROGRESS_SAVE_WORLDS);
                }
                catch (MinecraftException minecraftexception)
                {
                	CrashReport crashreport = CrashReport.makeCrashReport(minecraftexception, "Exception saving worlds");
                    worldserver.addWorldInfoToCrashReport(crashreport);
                    throw new ReportedException(crashreport);
                }
            }
        }
    }
}
