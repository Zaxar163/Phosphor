package ru.zaxar163.phosphor.mixins.fixes.common;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraftforge.common.util.EnumHelper;
import ru.zaxar163.phosphor.PrivillegedBridge;

@Mixin(EnumHelper.class)
public class MixinEnumHelper {
	private static MethodHandle MODIFIERS_SETTER;

	private static void setup0() {
		Field modifiersField;
		try {
			modifiersField = PrivillegedBridge.getField(Field.class, "modifiers");
	        modifiersField.setAccessible(true);
	        MODIFIERS_SETTER = MethodHandles.publicLookup().unreflectSetter(modifiersField);
		} catch (Throwable e) {
			throw new Error(e);
		}
	}
	/*
	@Overwrite
    public static void setFailsafeFieldValue(Field field, @Nullable Object target, @Nullable Object value) throws Exception
    {
        field.setAccessible(true);
        	try {
				MODIFIERS_SETTER.invokeExact(field, field.getModifiers() & ~Modifier.FINAL);
			} catch (Throwable e) {
				// WTF
				throw new Error(e);
			}
        	try {
				MethodHandles.publicLookup().unreflectSetter(field).invoke(target, value);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
    }*/
}
