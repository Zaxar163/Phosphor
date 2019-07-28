package ru.zaxar163.phosphor.mixins.plugins;

import net.minecraft.launchwrapper.Launch;
import ru.zaxar163.phosphor.PhosphorConfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class OptimEnginePlugin implements IMixinConfigPlugin {
    private static final Logger logger = LogManager.getLogger("Phosphor Plugin");

    public static boolean ENABLE_ILLEGAL_THREAD_ACCESS_WARNINGS = false;

    public static PhosphorConfig CFG = new PhosphorConfig();
    
    private PhosphorConfig config;

    private boolean spongePresent;

    @Override
    public void onLoad(String mixinPackage) {
        logger.info("Loading configuration");

        this.config = PhosphorConfig.loadConfig();
        CFG = config;

        ENABLE_ILLEGAL_THREAD_ACCESS_WARNINGS = this.config.enableIllegalThreadAccessWarnings;

        try {
            Class.forName("org.spongepowered.mod.SpongeCoremod");

            this.spongePresent = true;
        } catch (Exception e) {
            this.spongePresent = false;
        }

        if (this.spongePresent) {
            logger.info("Sponge has been detected on the classpath! Sponge mixins will be used.");
            logger.warn("Please keep in mind that Sponge support is **experimental** (although supported). We cannot currently" +
                    "detect if you are using Sponge's async lighting feature, so please disable it if you have not already.");
        }
    }

    @Override
    public String getRefMapperConfig() {
        if (Launch.blackboard.get("fml.deobfuscatedEnvironment") == Boolean.TRUE) {
            return null;
        }

        return "mixins.phosphor.refmap.json";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String[] nameParts = mixinClassName.split("\\.");
        if (!this.config.enableLightOptim && nameParts[nameParts.length-3].equals("lighting")) return false;
        if (!this.config.enablePacketLoogger && nameParts[nameParts.length-3].equals("packetlog")) return false;

        if (this.spongePresent) {
            if (nameParts[nameParts.length-1].endsWith("$Vanilla")) {
                logger.info("Disabled mixin '{}' as we are in a Sponge environment", mixinClassName);
                return false;
            }
        } else {
            if (nameParts[nameParts.length-1].endsWith("$Sponge")) {
                logger.info("Disabled mixin '{}' as we are in a basic Forge environment", mixinClassName);
                return false;
            }
        }

        return checkCurentEnv(nameParts[nameParts.length-2], mixinClassName);
    }

    private boolean checkCurentEnv(String substring, String fullName) {
    	if (substring.equals("server") && MixinEnvironment.getCurrentEnvironment().getSide() != MixinEnvironment.Side.SERVER) {
    		logger.info("Disabled mixin '{}' as we are in client environment", fullName);
    		return false;
    	}
    	if (substring.equals("client") && MixinEnvironment.getCurrentEnvironment().getSide() != MixinEnvironment.Side.CLIENT) {
    		logger.info("Disabled mixin '{}' as we are in server environment", fullName);
    		return false;
    	}
    	return true;
	}

	@Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
