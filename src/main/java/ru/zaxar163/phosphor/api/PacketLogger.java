package ru.zaxar163.phosphor.api;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.server.SPacketAdvancementInfo;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class PacketLogger {
	private static volatile List<Package> vacantPackage;

	static {
		vacantPackage = new ArrayList<>(Arrays.asList(SPacketAdvancementInfo.class.getPackage(), CPacketAnimation.class.getPackage()));
		Arrays.stream(System.getProperty("logger.overPkgs", "").split(File.pathSeparator)).map(PrivillegedBridge::firstClass).map(e -> e.getPackage()).forEach(vacantPackage::add);
	}

	public static ItemStack record(ItemStack stack, Class<?>[] context) {
		Class<?> caller = context[2];
		if (caller == ByteBufUtils.class) caller = context[3];
		if (vacantPackage.contains(caller.getPackage())) return stack;
		StringBuilder toLog = new StringBuilder("Tried to read itemstack from BB it is bad practice for modders:\n");
		for (int i = 2; i < context.length; i++) toLog.append(context[i]).append('\n');
		FMLLog.log.warn(toLog);
		return stack;
	}
}
