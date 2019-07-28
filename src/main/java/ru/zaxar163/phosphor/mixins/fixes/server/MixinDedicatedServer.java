package ru.zaxar163.phosphor.mixins.fixes.server;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.server.dedicated.DedicatedServer;

@Mixin(DedicatedServer.class)
public class MixinDedicatedServer {
	@Overwrite
	public void setGuiEnabled() {
	}
}
