package ru.zaxar163.phosphor.mixins.packetlog.common;

import java.io.IOException;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import ru.zaxar163.phosphor.api.PacketLogger;
import ru.zaxar163.phosphor.api.PrivillegedBridge.TraceSecurityManager;

@Mixin(PacketBuffer.class)
public abstract class MixinPacketBuffer {
	@Shadow @Final private ByteBuf buf;

	@Overwrite
	public ItemStack readItemStack() throws IOException
    {
        int i = this.readShort();

        if (i < 0)
        {
            return ItemStack.EMPTY;
        }
        else
        {
            int j = this.readByte();
            int k = this.readShort();
            Item it = Item.getItemById(i);
            if (it == null) {
            	return ItemStack.EMPTY;  //antiNPE flood
            }
            ItemStack itemstack = new ItemStack(it, j, k);
            itemstack.getItem().readNBTShareTag(itemstack, this.readCompoundTag());
            PacketLogger.record(itemstack, TraceSecurityManager.INSTANCE.getClassContext());
            return itemstack;
        }
    }

	@Shadow protected abstract NBTTagCompound readCompoundTag();
	@Shadow protected abstract byte readByte();
	@Shadow protected abstract short readShort();
}
