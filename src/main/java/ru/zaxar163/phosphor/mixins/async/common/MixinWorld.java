package ru.zaxar163.phosphor.mixins.async.common;

import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import ru.zaxar163.phosphor.api.AsyncTick;
import ru.zaxar163.phosphor.api.PrivillegedBridge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Mixin(World.class)
public abstract class MixinWorld {
	@Redirect(method = "updateEntities", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", remap = false),
        slice = @Slice(from = @At(value="INVOKE",
                target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V"),
                to = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getRidingEntity()Lnet/minecraft/entity/Entity;"
        )))
    public int entityRedir(List<Entity> loadedEntityList) {
    	for (Entity heh : loadedEntityList)
        {
    		AsyncTick.INSTANCE.run(() -> {
            Entity entity2 = heh;
            Entity entity3 = entity2.getRidingEntity();

            if (entity3 != null)
            {
                if (!entity3.isDead && entity3.isPassenger(entity2))
                {
                    return;
                }

                entity2.dismountRidingEntity();
            }

            this.profiler.startSection("tick");

            if (!entity2.isDead && !(entity2 instanceof EntityPlayerMP))
            {
                try
                {
                    net.minecraftforge.server.timings.TimeTracker.ENTITY_UPDATE.trackStart(entity2);
                    this.updateEntity(entity2);
                    net.minecraftforge.server.timings.TimeTracker.ENTITY_UPDATE.trackEnd(entity2);
                }
                catch (Throwable throwable1)
                {
                    CrashReport crashreport1 = CrashReport.makeCrashReport(throwable1, "Ticking entity");
                    CrashReportCategory crashreportcategory1 = crashreport1.makeCategory("Entity being ticked");
                    entity2.addEntityCrashInfo(crashreportcategory1);
                    if (net.minecraftforge.common.ForgeModContainer.removeErroringEntities)
                    {
                        net.minecraftforge.fml.common.FMLLog.log.fatal("{}", crashreport1.getCompleteReport());
                        removeEntity(entity2);
                    }
                    else
                    throw new ReportedException(crashreport1);
                }
            }

            this.profiler.endSection();
            this.profiler.startSection("remove");

            if (entity2.isDead)
            {
                int l1 = entity2.chunkCoordX;
                int i2 = entity2.chunkCoordZ;

                if (entity2.addedToChunk && this.isChunkLoaded(l1, i2, true))
                {
                    this.getChunk(l1, i2).removeEntity(entity2);
                }

                loadedEntityList.remove(entity2);
                this.onEntityRemoved(entity2);
            }

            this.profiler.endSection();
        	});
        }
        return 0;
    }

	@Redirect(method = "updateEntities", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", remap = false),
	        slice = @Slice(from = @At(value="INVOKE",
	                target = "Lnet/minecraft/tileentity/TileEntity;onChunkUnload()V"),
	                to = @At(value = "INVOKE",
	                target = "Lnet/minecraft/tileentity/TileEntity;isInvalid()Z"
	        )))
	public Iterator<TileEntity> tileEntityRedir(List<TileEntity> tickableTileEntities) {
		Iterator<TileEntity> iterator = tickableTileEntities.iterator();

        while (iterator.hasNext())
        {
            TileEntity tileentity = iterator.next();

            if (!tileentity.isInvalid() && tileentity.hasWorld())
            {
            	AsyncTick.INSTANCE.run(() -> {
                BlockPos blockpos = tileentity.getPos();

                if (this.isBlockLoaded(blockpos, false) && this.worldBorder.contains(blockpos)) //Forge: Fix TE's getting an extra tick on the client side....
                {
                    try
                    {
                        this.profiler.func_194340_a(() ->
                        {
                            return String.valueOf((Object)TileEntity.getKey(tileentity.getClass()));
                        });
                        net.minecraftforge.server.timings.TimeTracker.TILE_ENTITY_UPDATE.trackStart(tileentity);
                        ((ITickable)tileentity).update();
                        net.minecraftforge.server.timings.TimeTracker.TILE_ENTITY_UPDATE.trackEnd(tileentity);
                        this.profiler.endSection();
                    }
                    catch (Throwable throwable)
                    {
                        CrashReport crashreport2 = CrashReport.makeCrashReport(throwable, "Ticking block entity");
                        CrashReportCategory crashreportcategory2 = crashreport2.makeCategory("Block entity being ticked");
                        tileentity.addInfoToCrashReport(crashreportcategory2);
                        if (net.minecraftforge.common.ForgeModContainer.removeErroringTileEntities)
                        {
                            net.minecraftforge.fml.common.FMLLog.log.fatal("{}", crashreport2.getCompleteReport());
                            tileentity.invalidate();
                            this.removeTileEntity(tileentity.getPos());
                        }
                        else
                        throw new ReportedException(crashreport2);
                    }
                }
                // Recheck
                if (tileentity.isInvalid())
                {
                    iterator.remove();
                    this.loadedTileEntityList.remove(tileentity);

                    if (this.isBlockLoaded(tileentity.getPos()))
                    {
                        //Forge: Bugfix: If we set the tile entity it immediately sets it in the chunk, so we could be desyned
                        Chunk chunk = this.getChunk(tileentity.getPos());
                        if (chunk.getTileEntity(tileentity.getPos(), net.minecraft.world.chunk.Chunk.EnumCreateEntityType.CHECK) == tileentity)
                            chunk.removeTileEntity(tileentity.getPos());
                    }
                }
            	});
            }

            if (tileentity.isInvalid())
            {
                iterator.remove();
                this.loadedTileEntityList.remove(tileentity);

                if (this.isBlockLoaded(tileentity.getPos()))
                {
                    //Forge: Bugfix: If we set the tile entity it immediately sets it in the chunk, so we could be desyned
                    Chunk chunk = this.getChunk(tileentity.getPos());
                    if (chunk.getTileEntity(tileentity.getPos(), net.minecraft.world.chunk.Chunk.EnumCreateEntityType.CHECK) == tileentity)
                        chunk.removeTileEntity(tileentity.getPos());
                }
            }
        }
		return Collections.emptyIterator();
	}

    
	@Shadow protected abstract Chunk getChunk(BlockPos pos);
	@Shadow protected abstract void removeTileEntity(BlockPos pos);
	@Shadow protected abstract boolean isBlockLoaded(BlockPos pos);
	@Shadow protected abstract boolean isBlockLoaded(BlockPos blockpos, boolean b);
	@Shadow protected abstract boolean isChunkLoaded(int l1, int i2, boolean b);
    @Shadow protected abstract Chunk getChunk(int l1, int i2);
	@Shadow protected abstract void updateEntity(Entity entity2);
	@Shadow protected abstract void removeEntity(Entity entityIn);
	@Shadow protected abstract void onEntityRemoved(Entity entityIn);
	@Shadow public Profiler profiler;
	@Shadow public WorldBorder worldBorder;

	@Inject(method = "<init>", at = @At("RETURN"))
	public void construct(ISaveHandler saveHandlerIn, WorldInfo info, WorldProvider provider, Profiler profilerIn, boolean unused, CallbackInfo ci) {
		loadedEntityList = PrivillegedBridge.arrayList();
		unloadedEntityList = PrivillegedBridge.arrayList();
		loadedTileEntityList = PrivillegedBridge.arrayList();
		tickableTileEntities = PrivillegedBridge.arrayList();
		addedTileEntityList = PrivillegedBridge.arrayList();
		tileEntitiesToBeRemoved = PrivillegedBridge.arrayList();
		playerEntities = PrivillegedBridge.arrayList();
	}

    @Shadow public List<Entity> loadedEntityList;
    @Shadow public List<Entity> unloadedEntityList;
    @Shadow public List<TileEntity> loadedTileEntityList;
    @Shadow public List<TileEntity> tickableTileEntities;
    @Shadow public List<TileEntity> addedTileEntityList;
    @Shadow public List<TileEntity> tileEntitiesToBeRemoved;
    @Shadow public List<EntityPlayer> playerEntities;
}
