package ru.zaxar163.phosphor.mixins.async.common;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ReportedException;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import ru.zaxar163.phosphor.api.AsyncTick;
import ru.zaxar163.phosphor.mixins.plugins.OptimEnginePlugin;

@Mixin(WorldServer.class)
public abstract class MixinWorldServer extends World {
	@Shadow private TreeSet<NextTickListEntry> pendingTickListEntriesTreeSet;
	@Shadow private Set<NextTickListEntry> pendingTickListEntriesHashSet;
	protected MixinWorldServer(ISaveHandler saveHandlerIn, WorldInfo info, WorldProvider providerIn,
			Profiler profilerIn, boolean client) {
		super(saveHandlerIn, info, providerIn, profilerIn, client);
	}
	
	@Inject(method = "<init>", at = @At("RETURN"))
	public void construct(MinecraftServer server, ISaveHandler saveHandlerIn, WorldInfo info, int dimensionId, Profiler profilerIn, CallbackInfo ci) {
		if (OptimEnginePlugin.CFG.asyncEntity) {
		pendingTickListEntriesHashSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
		try {
			pendingTickListEntriesTreeSet = (TreeSet<NextTickListEntry>) AsyncTick.treeSetConstructor.invoke(new ConcurrentSkipListMap<>());
		} catch (Throwable e) {
			throw new Error(e); // never happen
		}
		}
	}
	
	@Overwrite
    protected void tickPlayers()
    {
        super.tickPlayers();
        this.profiler.endStartSection("players");

        for (int i = 0; i < this.playerEntities.size(); ++i)
        {
            Entity entity = this.playerEntities.get(i);
            Entity entity1 = entity.getRidingEntity();

            if (entity1 != null)
            {
                if (!entity1.isDead && entity1.isPassenger(entity))
                {
                    continue;
                }

                entity.dismountRidingEntity();
            }

            this.profiler.startSection("tick");

            if (!entity.isDead)
            {
            	Runnable r = () -> {
                try
                {
                    this.updateEntity(entity);
                }
                catch (Throwable throwable)
                {
                    CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Ticking player");
                    CrashReportCategory crashreportcategory = crashreport.makeCategory("Player being ticked");
                    entity.addEntityCrashInfo(crashreportcategory);
                    throw new ReportedException(crashreport);
                }
            	};
            	if (OptimEnginePlugin.CFG.asyncPlayers) AsyncTick.INSTANCE.forceRunS(r);
            	else r.run();
            }

            this.profiler.endSection();
            this.profiler.startSection("remove");

            if (entity.isDead)
            {
                int j = entity.chunkCoordX;
                int k = entity.chunkCoordZ;

                if (entity.addedToChunk && this.isChunkLoaded(j, k, true))
                {
                    this.getChunk(j, k).removeEntity(entity);
                }

                this.loadedEntityList.remove(entity);
                this.onEntityRemoved(entity);
            }

            this.profiler.endSection();
        }
    }

}
