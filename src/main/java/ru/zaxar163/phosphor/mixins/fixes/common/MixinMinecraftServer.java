package ru.zaxar163.phosphor.mixins.fixes.common;

import net.minecraft.advancements.FunctionManager;
import net.minecraft.crash.CrashReport;
import net.minecraft.network.NetworkSystem;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.ITickable;
import net.minecraft.util.ReportedException;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;
import ru.zaxar163.phosphor.PhosphorData;
import ru.zaxar163.phosphor.core.PhosphorFMLSetupHook;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.FutureTask;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {
	private static final Integer[] EMPTY_INT = new Integer[0];
	//@Shadow @Final private static Logger LOGGER;
	@Shadow public WorldServer[] worlds;
	@Shadow private int tickCounter;
	@Shadow private PlayerList playerList;
	@Shadow private List<ITickable> tickables;
	@Shadow private Profiler profiler;
	@Shadow protected abstract boolean getAllowNether();
	@Shadow @Final public Queue < FutureTask<? >> futureTaskQueue;
	@Shadow public java.util.Hashtable<Integer, long[]> worldTickTimes;
	@Shadow public abstract NetworkSystem getNetworkSystem();
	@Shadow public abstract FunctionManager getFunctionManager();
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
                	PhosphorFMLSetupHook.logger.info("Saving chunks for level '{}'/{}", worldserver.getWorldInfo().getWorldName(), worldserver.provider.getDimensionType().getName());
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
    
    @Redirect(method = "updateTimeLightAndEntities", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/DimensionManager;getIDs(Z)[Ljava/lang/Integer;"))
    public Integer[] updateTimeLightAndEntities(boolean unused)
    {
        Integer[] ids = net.minecraftforge.common.DimensionManager.getIDs(this.tickCounter % 200 == 0);
        for (int x = 0; x < ids.length; x++)
        {
            int id = ids[x];
            long i = System.nanoTime();

            if (id == 0 || this.getAllowNether())
            {
                WorldServer worldserver = net.minecraftforge.common.DimensionManager.getWorld(id);
                this.profiler.func_194340_a(() ->
                {
                    return worldserver.getWorldInfo().getWorldName();
                });

                if (this.tickCounter % 20 == 0)
                {
                    this.profiler.startSection("timeSync");
                    this.playerList.sendPacketToAllPlayersInDimension(new SPacketTimeUpdate(worldserver.getTotalWorldTime(), worldserver.getWorldTime(), worldserver.getGameRules().getBoolean("doDaylightCycle")), worldserver.provider.getDimension());
                    this.profiler.endSection();
                }

                this.profiler.startSection("tick");
                net.minecraftforge.fml.common.FMLCommonHandler.instance().onPreWorldTick(worldserver);

                try
                {
                    worldserver.tick();
                }
                catch (Throwable throwable1)
                {
                    CrashReport crashreport = CrashReport.makeCrashReport(throwable1, "Exception ticking world");
                    worldserver.addWorldInfoToCrashReport(crashreport);
                    throw new ReportedException(crashreport);
                }

                try
                {
                    worldserver.updateEntities();
                }
                catch (Throwable throwable)
                {
                    CrashReport crashreport1 = CrashReport.makeCrashReport(throwable, "Exception ticking world entities");
                    worldserver.addWorldInfoToCrashReport(crashreport1);
                    throw new ReportedException(crashreport1);
                }

                net.minecraftforge.fml.common.FMLCommonHandler.instance().onPostWorldTick(worldserver);
                this.profiler.endSection();
                this.profiler.startSection("tracker");
                worldserver.getEntityTracker().tick();
                this.profiler.endSection();
                this.profiler.endSection();
            }
            worldTickTimes.get(id)[this.tickCounter % 100] = System.nanoTime() - i;
        }
        return EMPTY_INT;
    }
}
