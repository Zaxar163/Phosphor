package ru.zaxar163.phosphor;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import net.minecraft.launchwrapper.Launch;

public class PhosphorConfig {
	private static final Gson gson = createGson();

	public static final File CONF_DIR = new File(Launch.minecraftHome, "config");
	private static final File CONF_FILE = new File(CONF_DIR, "phosphor.json");

	private static Gson createGson() {
		return new GsonBuilder().setPrettyPrinting().create();
	}

	public static PhosphorConfig loadConfig() {
		if (!CONF_FILE.exists()) {
			PhosphorConfig config = new PhosphorConfig();
			config.saveConfig();

			return config;
		}

		try (Reader reader = new FileReader(CONF_FILE)) {
			return gson.fromJson(reader, PhosphorConfig.class);
		} catch (IOException e) {
			throw new RuntimeException("Failed to deserialize config from disk", e);
		}
	}

	@SerializedName("enable_illegal_thread_access_warnings")
	public boolean enableIllegalThreadAccessWarnings = false;

	@SerializedName("enable_async_lighting")
	public boolean enableLightOptim = true;

	@SerializedName("enable_packetLoggerr")
	public boolean enablePacketLoogger = true;

	private void saveConfig() {
		if (!CONF_DIR.exists()) {
			if (!CONF_DIR.mkdirs())
				throw new RuntimeException(
						"Could not create configuration directory at '" + CONF_DIR.getAbsolutePath() + "'");
		} else if (!CONF_DIR.isDirectory())
			throw new RuntimeException(
					"Configuration directory at '" + CONF_DIR.getAbsolutePath() + "' is not a directory");

		try (Writer writer = new FileWriter(CONF_FILE)) {
			gson.toJson(this, writer);
		} catch (IOException e) {
			throw new RuntimeException("Failed to serialize config to disk", e);
		}
	}
}
