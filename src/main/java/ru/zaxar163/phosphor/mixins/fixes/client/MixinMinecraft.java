package ru.zaxar163.phosphor.mixins.fixes.client;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.authlib.properties.PropertyMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.profiler.ISnooperInfo;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.ScreenshotEvent;
import ru.zaxar163.phosphor.PhosphorData;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft implements IThreadListener, ISnooperInfo {
	@Shadow
	@Final
	private static Logger LOGGER;
	@Shadow
	private static Minecraft instance;

	@Shadow
	@Final
	public Profiler profiler;

	@Shadow
	public GuiIngame ingameGUI;

	@Shadow
	@Final
	private PropertyMap profileProperties;

	/**
	 * @reason Fix GUI logic being included as part of "root.tick.textures"
	 *         (https://bugs.mojang.com/browse/MC-129556)
	 */
	@Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", ordinal = 0))
	private void endStartGUISection(Profiler profiler, String name) {
		profiler.endStartSection("gui");
	}

	@Overwrite
	public PropertyMap getProfileProperties() {
		try {
			instance.getProfileProperties();
		} catch (Throwable t) { // simple ignore only for stop flooding in dev env
		}
		return profileProperties;
	}

	@Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;instance:Lnet/minecraft/client/Minecraft;"))
	private void onClientInit(Minecraft minecraft) throws IllegalAccessException {
		instance = minecraft;
		final Thread thread = Thread.currentThread();
		PhosphorData.CLIENT.set(thread);
	}

	/**
	 * @reason Make saving screenshots async
	 *         (https://bugs.mojang.com/browse/MC-33383)
	 */
	@Redirect(method = "dispatchKeypresses", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ScreenShotHelper;saveScreenshot(Ljava/io/File;IILnet/minecraft/client/shader/Framebuffer;)Lnet/minecraft/util/text/ITextComponent;", ordinal = 0))
	private ITextComponent saveScreenshotAsync(File gameDirectory, int width, int height, Framebuffer buffer) {
		try {
			final BufferedImage screenshot = ScreenShotHelper.createScreenshot(width, height, buffer);

			new Thread(() -> {
				try {
					File screenshotDir = new File(gameDirectory, "screenshots");
					screenshotDir.mkdir();
					File screenshotFile = ScreenShotHelper.getTimestampedPNGFileForDirectory(screenshotDir)
							.getCanonicalFile();

					// Forge event
					ScreenshotEvent event = ForgeHooksClient.onScreenshot(screenshot, screenshotFile);
					if (event.isCanceled()) {
						ingameGUI.getChatGUI().printChatMessage(event.getCancelMessage());
						return;
					} else
						screenshotFile = event.getScreenshotFile();

					ImageIO.write(screenshot, "png", screenshotFile);

					// Forge event
					if (event.getResultMessage() != null) {
						ingameGUI.getChatGUI().printChatMessage(event.getResultMessage());
						return;
					}

					ITextComponent screenshotLink = new TextComponentString(screenshotFile.getName());
					screenshotLink.getStyle().setClickEvent(
							new ClickEvent(ClickEvent.Action.OPEN_FILE, screenshotFile.getAbsolutePath()));
					screenshotLink.getStyle().setUnderlined(true);
					ingameGUI.getChatGUI()
							.printChatMessage(new TextComponentTranslation("screenshot.success", screenshotLink));
				} catch (Exception e) {
					LOGGER.warn("Couldn't save screenshot", e);
					ingameGUI.getChatGUI()
							.printChatMessage(new TextComponentTranslation("screenshot.failure", e.getMessage()));
				}
			}, "Screenshot Saving Thread").start();
		} catch (Exception e) {
			LOGGER.warn("Couldn't save screenshot", e);
			ingameGUI.getChatGUI().printChatMessage(new TextComponentTranslation("screenshot.failure", e.getMessage()));
		}

		return null;
	}

	/** @reason Message is sent from screenshot method now. */
	@Redirect(method = "dispatchKeypresses", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiNewChat;printChatMessage(Lnet/minecraft/util/text/ITextComponent;)V", ordinal = 0))
	private void sendScreenshotMessage(GuiNewChat guiNewChat, ITextComponent chatComponent) {
	}

	/** @reason Part 2 of GUI logic fix. */
	@Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;tick()V", ordinal = 0))
	private void tickTextureManagerWithCorrectProfiler(TextureManager textureManager) {
		profiler.endStartSection("textures");
		textureManager.tick();
		profiler.endStartSection("gui");
	}
}
