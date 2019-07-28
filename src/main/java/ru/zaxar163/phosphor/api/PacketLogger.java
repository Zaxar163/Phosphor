package ru.zaxar163.phosphor.api;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.item.ItemStack;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.server.SPacketAdvancementInfo;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class PacketLogger {
	public static final Logger log;
	private static volatile List<Package> vacantPackage;

	static {
		log = LogManager.getLogger("PacketLogger");
		vacantPackage = new ArrayList<>(
				Arrays.asList(SPacketAdvancementInfo.class.getPackage(), CPacketAnimation.class.getPackage()));
		if (!System.getProperty("logger.overPkgs", "").isEmpty())
			Arrays.stream(System.getProperty("logger.overPkgs").split(File.pathSeparator))
					.map(PrivillegedBridge::firstClass).map(e -> e.getPackage()).forEach(vacantPackage::add);
	}

	public static ItemStack record(ItemStack stack, Class<?>[] context) {
		int shift = 2;
		if (context[shift] == ByteBufUtils.class)
			shift++;
		Class<?> caller = context[shift];
		if (vacantPackage.contains(caller.getPackage()))
			return stack;
		StringBuilder toLog = new StringBuilder(
				"Tried to read itemstack from PacketBuffer it is bad practice for modders:\n");
		toLog.append("\tItem: ").append(stack.getItem().getTranslationKey()).append(", damage: ")
				.append(stack.getItemDamage()).append(", meta: ").append(stack.getMetadata()).append('\n');
		toLog.append("\tStack:\n");
		for (int i = shift; i < context.length && context[i] != NetworkManager.class; i++)
			toLog.append("\t\t").append(context[i].getName()).append('\n');
		log.error(toLog);
		return stack;
	}
}
