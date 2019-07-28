package ru.zaxar163.phosphor.core;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import net.minecraftforge.fml.relauncher.IFMLCallHook;

public class PhosphorFMLSetupHook implements IFMLCallHook {
	public static final Logger logger = LogManager.getLogger("Phosphor Forge Core");

	@Override
	public Void call() {
		logger.info("Phosphor has been hooked by Forge, setting up Mixin and plugins");

		MixinBootstrap.init();

		Mixins.addConfiguration("mixins.phosphor.json");

		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
	}
}
