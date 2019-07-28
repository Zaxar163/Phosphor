package ru.zaxar163.phosphor;

import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;

public final class PhosphorContainer extends DummyModContainer {
	public PhosphorContainer() {
		super(new ModMetadata());
		ModMetadata meta = getMetadata();
		meta.modId = PhosphorConstants.MOD_ID;
		meta.name = PhosphorConstants.MOD_NAME;
		meta.version = PhosphorConstants.MOD_VERSION;
		meta.credits = "jellysquid3";
		meta.authorList = Arrays.asList("jellysquid3", "Zaxar163");
		meta.description = "Lots of fixes for minecraft...";
		meta.url = "https://github.com/Zaxar163/Phosphor/";
		meta.screenshots = new String[0];
		meta.logoFile = "";
	}

	@NetworkCheckHandler
	public boolean checkModLists(Map<String, String> modList, Side side) {
		return true;
	}

	@Override
	public Object getMod() {
		return this;
	}

	@Override
	@Nullable
	public Certificate getSigningCertificate() {
		return null;
	}

	@Override
	public boolean matches(Object mod) {
		return mod == this;
	}

	@Subscribe
	public void modConstruction(FMLConstructionEvent evt) {
	}

	@Subscribe
	public void modInitialization(FMLInitializationEvent evt) {
		MinecraftForge.EVENT_BUS.register(new PhosphorHooks());
	}

	@Subscribe
	public void modPostinitialization(FMLPostInitializationEvent evt) {
	}

	@Subscribe
	public void modPreinitialization(FMLPreInitializationEvent evt) {
	}

	@Override
	public boolean registerBus(EventBus bus, LoadController controller) {
		bus.register(this);
		return true;
	}
}
