package ru.zaxar163.phosphor;

import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Map;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import javax.annotation.Nullable;


public final class PhosphotContainer extends DummyModContainer
{
    public PhosphotContainer()
    {
        super(new ModMetadata());
        ModMetadata meta = getMetadata();
        meta.modId=PhosphorConstants.MOD_ID;
        meta.name=PhosphorConstants.MOD_NAME;
        meta.version=PhosphorConstants.MOD_VERSION;
        meta.credits="jellysquid3";
        meta.authorList=Arrays.asList("jellysquid3", "Zaxar163");
        meta.description="Lots of fixes for minecraft...";
        meta.url="https://github.com/Zaxar163/Phosphor/";
        meta.screenshots=new String[0];
        meta.logoFile="";
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller)
    {
        bus.register(this);
        return true;
    }

    @Subscribe
    public void modConstruction(FMLConstructionEvent evt)
    {
    }

    @Subscribe
    public void modPreinitialization(FMLPreInitializationEvent evt)
    {
    }

    @NetworkCheckHandler
    public boolean checkModLists(Map<String,String> modList, Side side)
    {
    	return true;
    }

    @Override
    @Nullable
    public Certificate getSigningCertificate()
    {
        return null;
    }

    @Override
    public boolean matches(Object mod)
    {
        return mod == this;
    }

    @Override
    public Object getMod()
    {
        return this;
    }
}
