package ru.zaxar163.phosphor.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import ru.zaxar163.phosphor.api.PrivillegedBridge;

import javax.annotation.Nullable;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public class PhosphorFMLLoadingPlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return "ru.zaxar163.phosphor.PhosphorContainer";
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return PhosphorFMLSetupHook.class.getName();
    }

    @Override
    public void injectData(Map<String, Object> data) {
    	try {
			Class.forName(PrivillegedBridge.class.getName());
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
