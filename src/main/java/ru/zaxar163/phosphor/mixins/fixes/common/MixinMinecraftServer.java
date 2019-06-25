package ru.zaxar163.phosphor.mixins.fixes.common;

import net.minecraft.crash.CrashReport;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.ITickable;
import net.minecraft.util.ReportedException;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;
import ru.zaxar163.phosphor.PhosphorData;

import java.io.File;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer extends MinecraftServer {
	public MixinMinecraftServer(File anvilFileIn, Proxy proxyIn, DataFixer dataFixerIn,
			YggdrasilAuthenticationService authServiceIn, MinecraftSessionService sessionServiceIn,
			GameProfileRepository profileRepoIn, PlayerProfileCache profileCacheIn) {
		super(anvilFileIn, proxyIn, dataFixerIn, authServiceIn, sessionServiceIn, profileRepoIn, profileCacheIn);
	}

	@Shadow @Final private static Logger LOGGER;
	@Shadow public WorldServer[] worlds;
	@Shadow private int tickCounter;
	@Shadow private PlayerList playerList;
	@Shadow private List<ITickable> tickables;
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
    
    @Overwrite
    public void updateTimeLightAndEntities()
    {
        this.profiler.startSection("jobs");

        synchronized (this.futureTaskQueue)
        {
            while (!this.futureTaskQueue.isEmpty())
            {
                Util.runTask(this.futureTaskQueue.poll(), LOGGER);
            }
        }

        this.profiler.endStartSection("levels");
        net.minecraftforge.common.chunkio.ChunkIOExecutor.tick();
        List<Thread> threads = new ArrayList<>();
        Integer[] ids = net.minecraftforge.common.DimensionManager.getIDs(this.tickCounter % 200 == 0);
        for (int x = 0; x < ids.length; x++)
        {
            int id = ids[x];
            long i = System.nanoTime();

            if (id == 0 || this.getAllowNether())
            {
            	threads.add(new Thread(Thread.currentThread().getThreadGroup(), () -> {
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
            	}, "World tick thread..."));
            }
            worldTickTimes.get(id)[this.tickCounter % 100] = System.nanoTime() - i;
        }
        threads.forEach(t -> {
			try {
				t.join();
			} catch (InterruptedException ign) { }
		});
        this.profiler.endStartSection("dim_unloading");
        net.minecraftforge.common.DimensionManager.unloadWorlds(worldTickTimes);
        this.profiler.endStartSection("connection");
        this.getNetworkSystem().networkTick();
        this.profiler.endStartSection("players");
        this.playerList.onTick();
        this.profiler.endStartSection("commandFunctions");
        this.getFunctionManager().update();
        this.profiler.endStartSection("tickables");

        for (int k = 0; k < this.tickables.size(); ++k)
        {
            ((ITickable)this.tickables.get(k)).update();
        }

        this.profiler.endSection();
    }
}
