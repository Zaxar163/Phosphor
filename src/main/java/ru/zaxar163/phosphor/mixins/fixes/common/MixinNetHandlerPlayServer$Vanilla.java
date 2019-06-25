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
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.CPacketVehicleMove;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer$Vanilla {

    @Shadow public EntityPlayerMP player;

    /**
     * @author Aikar - September 21st, 2018
     * @reason Player positions could become desynced with their vehicle
     * resulting in chunk conflicts about which chunk the entity should
     * really be in.
     *
     * @see <html>https://github.com/PaperMC/Paper/blob/fd1bd5223a461b6d98280bb8f2d67280a30dd24a/Spigot-Server-Patches/0378-Sync-Player-Position-to-Vehicles.patch</html>
     *
     * @param entity The entity (lowest vehicle being ridden)
     * @param x The x position being moved to
     * @param y The y position being moved to
     * @param z The z position being moved to
     * @param yaw The yaw
     * @param pitch The pitch
     * @param packetVehicleMove The vehicle move packet
     */
    @Redirect(
        method = "processVehicleMove",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;setPositionAndRotation(DDDFF)V"
        ),
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/entity/Entity;move(Lnet/minecraft/entity/MoverType;DDD)V"
            ),
            to = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/util/math/AxisAlignedBB;shrink(D)Lnet/minecraft/util/math/AxisAlignedBB;",
                ordinal = 1
            )
        )
    )
    private void onPlayerSetLocationReSyncVehicle(Entity entity, double x, double y, double z, float yaw, float pitch, CPacketVehicleMove packetVehicleMove) {
        entity.setPositionAndRotation(x, y, z, yaw, pitch);
        // Now set the location on the player to update ridden entities and the position in the world.
        this.player.setPositionAndRotation(x, y, z, yaw, pitch);

    }

    /**
     * @author Aikar - September 21st, 2018
     * @reason Player positions could become desynced with their vehicle
     * resulting in chunk conflicts about which chunk the entity should
     * really be in.
     *
     * @see <html>https://github.com/PaperMC/Paper/blob/fd1bd5223a461b6d98280bb8f2d67280a30dd24a/Spigot-Server-Patches/0378-Sync-Player-Position-to-Vehicles.patch</html>
     *
     * @param entity The entity (lowest vehicle being ridden)
     * @param x The x position being moved to
     * @param y The y position being moved to
     * @param z The z position being moved to
     * @param yaw The yaw
     * @param pitch The pitch
     */
    @Redirect(
        method = "processVehicleMove",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;setPositionAndRotation(DDDFF)V"
        ),
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Ljava/util/List;isEmpty()Z",
                ordinal = 1,
                remap = false
            ),
            to = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/network/play/server/SPacketMoveVehicle;<init>(Lnet/minecraft/entity/Entity;)V",
                ordinal = 1
            )
        )
    )
    private void onEntitySetRotationIfCancelled(Entity entity, double x, double y, double z, float yaw, float pitch) {
        entity.setPositionAndRotation(x, y, z, yaw, pitch);
        // then set the player again
        this.player.setPositionAndRotation(x, y, z, yaw, pitch);
    }
}