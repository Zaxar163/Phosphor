package ru.zaxar163.phosphor.mixins.async.common;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

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
import ru.zaxar163.phosphor.api.PrivillegedBridge;

@Mixin(WorldServer.class)
public abstract class MixinWorldServer extends World {
	@Shadow private TreeSet<NextTickListEntry> pendingTickListEntriesTreeSet;
	@Shadow private Set<NextTickListEntry> pendingTickListEntriesHashSet;
	@Shadow private List<NextTickListEntry> pendingTickListEntriesThisTick;
	protected MixinWorldServer(ISaveHandler saveHandlerIn, WorldInfo info, WorldProvider providerIn,
			Profiler profilerIn, boolean client) {
		super(saveHandlerIn, info, providerIn, profilerIn, client);
	}
	
	@Inject(method = "<init>", at = @At("RETURN"))
	public void construct(MinecraftServer server, ISaveHandler saveHandlerIn, WorldInfo info, int dimensionId, Profiler profilerIn, CallbackInfo ci) {
		pendingTickListEntriesHashSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
		pendingTickListEntriesThisTick = PrivillegedBridge.arrayList();
		pendingTickListEntriesTreeSet = AsyncTick.newTreeSet();
	}

	@Overwrite
    protected void tickPlayers()
    {
        super.tickPlayers();
        this.profiler.endStartSection("players");

        for (Entity entity : this.playerEntities)
        {
        	AsyncTick.INSTANCE.forceRun(() -> {
            Entity entity1 = entity.getRidingEntity();

            if (entity1 != null)
            {
                if (!entity1.isDead && entity1.isPassenger(entity))
                {
                    return;
                }

                entity.dismountRidingEntity();
            }

            this.profiler.startSection("tick");

            if (!entity.isDead)
            {
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
            });
        }
    }
}
