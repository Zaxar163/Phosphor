package ru.zaxar163.phosphor.mixins.fixes.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.network.status.server.SPacketPong;
import ru.zaxar163.phosphor.api.IPatchedSPacketPong;

@Mixin(SPacketPong.class)
public class MixinSPacketPong implements IPatchedSPacketPong {
	@Shadow
	private long clientTime;

	@Override
	public long getClientTime() {
		return clientTime;
	}
}
