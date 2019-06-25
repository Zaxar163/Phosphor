package ru.zaxar163.phosphor.mixins.async.common;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.util.ClassInheritanceMultiMap;

@Mixin(ClassInheritanceMultiMap.class)
public abstract class MixinClassInheritanceMultiMap<T> extends AbstractSet<T> {
    @Shadow private Map < Class<?>, List<T >> map;
    @Shadow private Set < Class<? >> knownKeys;
    @Shadow private List<T> values;
	@Inject(method = "<init>", at = @At("RETURN"), cancellable = true)
	public void construct(Class<T> baseClassIn, CallbackInfo ci) {
		values = new CopyOnWriteArrayList<>();
		knownKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());
		map = new ConcurrentHashMap<>();
		ci.cancel();
	}
}
