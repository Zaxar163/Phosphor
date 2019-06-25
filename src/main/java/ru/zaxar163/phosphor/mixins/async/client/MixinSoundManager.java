package ru.zaxar163.phosphor.mixins.async.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.ISoundEventListener;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.SoundSystemException;
import paulscode.sound.codecs.CodecJOrbis;
import paulscode.sound.libraries.LibraryLWJGLOpenAL;

@Mixin(SoundManager.class)
public class MixinSoundManager {
	@Shadow public static Marker LOG_MARKER = MarkerManager.getMarker("SOUNDS");
    @Shadow public static Logger LOGGER = LogManager.getLogger();
    @Shadow public static Set<ResourceLocation> UNABLE_TO_PLAY;
    @Shadow public SoundHandler sndHandler;
    @Shadow public GameSettings options;
    @Shadow public boolean loaded;
    @Shadow public int playTime;
    @Shadow public Map<String, ISound> playingSounds;
    @Shadow public Map<ISound, String> invPlayingSounds;
    @Shadow public Multimap<SoundCategory, String> categorySounds;
    @Shadow public List<ITickableSound> tickableSounds;
    @Shadow public Map<ISound, Integer> delayedSounds;
    @Shadow public Map<String, Integer> playingSoundsStopTime;
    @Shadow public List<ISoundEventListener> listeners;
    @Shadow public List<String> pausedChannels;

	@Inject(method = "<init>", at = @At("RETURN"), cancellable = true)
	public void construct(SoundHandler sndHandler, GameSettings options, CallbackInfo ci) {
		this.playingSounds = HashBiMap.<String, ISound>create();
        this.invPlayingSounds = Collections.synchronizedMap(((BiMap<String, ISound>) this.playingSounds).inverse());
        this.playingSounds = Collections.synchronizedMap(this.playingSounds);
        this.categorySounds = HashMultimap.<SoundCategory, String>create();
        this.tickableSounds = Collections.synchronizedList(Lists.<ITickableSound>newArrayList());
        this.delayedSounds = Collections.synchronizedMap(Maps.<ISound, Integer>newHashMap());
        this.playingSoundsStopTime = Collections.synchronizedMap(Maps.<String, Integer>newHashMap());
        this.listeners = Collections.synchronizedList(Lists.<ISoundEventListener>newArrayList());
        this.pausedChannels = Collections.synchronizedList(Lists.<String>newArrayList());
        this.sndHandler = sndHandler;
        this.options = options;

        try
        {
            SoundSystemConfig.addLibrary(LibraryLWJGLOpenAL.class);
            SoundSystemConfig.setCodec("ogg", CodecJOrbis.class);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.sound.SoundSetupEvent((SoundManager) (Object)  this));
        }
        catch (SoundSystemException soundsystemexception)
        {
            LOGGER.error(LOG_MARKER, "Error linking with the LibraryJavaSound plug-in", (Throwable)soundsystemexception);
        }
        ci.cancel();
	}
}
