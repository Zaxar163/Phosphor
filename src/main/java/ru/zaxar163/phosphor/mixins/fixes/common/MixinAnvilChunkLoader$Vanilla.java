
/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ru.zaxar163.phosphor.mixins.fixes.common;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import ru.zaxar163.phosphor.core.PhosphorFMLSetupHook;

import org.apache.logging.log4j.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = AnvilChunkLoader.class, priority = 999)
public class MixinAnvilChunkLoader$Vanilla {

    private List<Entity> toUpdate = new ArrayList<>();

    /**
     * @author Aikar - September 17th, 2018
     * @reason Prevents saving bad entities to chunk.
     * Because invalidated entities that aren't being updated
     * position wise are being saved to their previously tracked
     * chunks, this avoids the entity being saved incorrectly to
     * the wrong chunk.
     *
     * @see <html>https://github.com/PaperMC/Paper/blob/master/Spigot-Server-Patches/0311-Prevent-Saving-Bad-entities-to-chunks.patch</html>
     * @param chunkIn The chunk the entity is supposedly a part of
     * @param worldIn The world of the chunk and "hopefully" the entity
     * @param compound The compound to write to
     * @param ci The callback info
     */
    @Inject(method = "writeChunkToNBT", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldProvider;hasSkyLight()Z", shift = At.Shift.AFTER))
    private void onWriteChunk(Chunk chunkIn, World worldIn, NBTTagCompound compound, CallbackInfo ci) {
        this.toUpdate.clear();
    }

    /**
     * @author Aikar - September 17th, 2018
     * @reason Prevents saving bad entities to chunk.
     * Because invalidated entities that aren't being updated
     * position wise are being saved to their previously tracked
     * chunks, this avoids the entity being saved incorrectly to
     * the wrong chunk.
     *
     * @see <html>https://github.com/PaperMC/Paper/blob/master/Spigot-Server-Patches/0311-Prevent-Saving-Bad-entities-to-chunks.patch</html>
     * @param entity The entity being saved, potentially
     * @param compound The compound to write to
     * @param chunkIn The chunk the entity is supposedly a part of
     * @param worldIn The world of the chunk and "hopefully" the entity
     * @param chunkCompound The compound of the chunk
     */
    @Redirect(method = "writeChunkToNBT", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;writeToNBTOptional(Lnet/minecraft/nbt/NBTTagCompound;)Z"))
    private boolean onEntityWriteCheckForValidPosition(Entity entity, NBTTagCompound compound, Chunk chunkIn, World worldIn, NBTTagCompound chunkCompound) {
        final int ecx = (int) Math.floor(entity.posX) >> 4;
        final int ecz = (int) Math.floor(entity.posZ) >> 4;

        if (ecx != chunkIn.x || ecz != chunkIn.z) {
        	PhosphorFMLSetupHook.logger.log(Level.WARN, "{} is not in chunk ({}, {}) and is instead in chunk ({}, {}) within world {}, skipping "
                + "save. This is a bug fix to a vanilla bug. Do not report this to Sponge or Forge please.", entity, chunkIn.x, chunkIn.z,
                ecx, ecz, chunkIn.getWorld().getWorldInfo().getWorldName());
            this.toUpdate.add(entity);
            // Instead of telling the entity to write to the optional, we just return false.
            // then the compound is not added to the list, nor is the compound actually used
            // and we artificially continue.
            return false;
        }
        if (entity.isDead) {
            return false;
        }
        return entity.writeToNBTOptional(compound);
    }

    /**
     * @author Aikar - September 17th, 2018
     * @reason Prevents saving bad entities to chunk.
     * Because invalidated entities that aren't being updated
     * position wise are being saved to their previously tracked
     * chunks, this avoids the entity being saved incorrectly to
     * the wrong chunk.
     *
     * @see <html>https://github.com/PaperMC/Paper/blob/master/Spigot-Server-Patches/0311-Prevent-Saving-Bad-entities-to-chunks.patch</html>
     * @param nbtTagCompound The compound being told to store the tag (the chunk compound)
     * @param key The key (should be "Entities")
     * @param value The value (the entities tag list)
     * @param chunk The chunk being saved
     * @param world The world being saved (needed to update the world's chunk position tracking for the invalid entity
     * @param chunkCompound The compound of the chunk
     */
    @Redirect(
        method = "writeChunkToNBT",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/nbt/NBTTagCompound;setTag(Ljava/lang/String;Lnet/minecraft/nbt/NBTBase;)V"
        ),
        slice = @Slice(
            from = @At(
                value = "CONSTANT",
                args = "stringValue=Entities"
            ),
            to = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/chunk/Chunk;getTileEntityMap()Ljava/util/Map;"
            )
        )
    )
    private void onWriteEntities(NBTTagCompound nbtTagCompound, String key, NBTBase value, Chunk chunk, World world, NBTTagCompound chunkCompound) {
        if (!"Entities".equals(key)) {
            nbtTagCompound.setTag(key, value);
            return;
        }
        if (!this.toUpdate.isEmpty()) {
            for (Entity entity : this.toUpdate) {
                world.updateEntityWithOptionalForce(entity, false);
            }
            this.toUpdate.clear();
        }
        nbtTagCompound.setTag(key, value);

    }

}