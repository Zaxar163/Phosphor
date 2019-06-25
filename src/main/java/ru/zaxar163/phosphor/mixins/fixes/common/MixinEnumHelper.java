package ru.zaxar163.phosphor.mixins.fixes.common;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.FMLLog;
import ru.zaxar163.phosphor.api.PrivillegedBridge;

@Mixin(EnumHelper.class)
public class MixinEnumHelper {
	private static MethodHandle MODIFIERS_SETTER;
    @Shadow private static Object reflectionFactory;
    @Shadow private static boolean isSetup;
	private static MethodHandle newConstructorAccessorM;
	private static MethodHandle newInstanceM;
	private static MethodHandle newFieldAccessorM;
	private static MethodHandle fieldAccessorSetM;
    
	private static void setup0() throws Throwable {
		Field modifiersField;
		modifiersField = PrivillegedBridge.getField(Field.class, "modifiers");
	    MODIFIERS_SETTER = PrivillegedBridge.ALL_LOOKUP.unreflectSetter(modifiersField);
	}
	
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
        		Object fieldAccessor = newFieldAccessorM.invoke(reflectionFactory, field, false);
        		fieldAccessorSetM.invoke(fieldAccessor, target, value);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
    
    }
	
	@Overwrite
    private static < T extends Enum<? >> T makeEnum(Class<T> enumClass, @Nullable String value, int ordinal, Class<?>[] additionalTypes, @Nullable Object[] additionalValues) throws Exception
    {
        int additionalParamsCount = additionalValues == null ? 0 : additionalValues.length;
        Object[] params = new Object[additionalParamsCount + 2];
        params[0] = value;
        params[1] = ordinal;
        if (additionalValues != null)
        {
            System.arraycopy(additionalValues, 0, params, 2, additionalValues.length);
        }
        try {
			return enumClass.cast(newInstanceM.invoke(getConstructorAccessor(enumClass, additionalTypes), new Object[] {params}));
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
    }
	
	@Overwrite
    /*
     * Everything below this is found at the site below, and updated to be able to compile in Eclipse/Java 1.6+
     * Also modified for use in decompiled code.
     * Found at: http://niceideas.ch/roller2/badtrash/entry/java_create_enum_instances_dynamically
     */
    private static Object getConstructorAccessor(Class<?> enumClass, Class<?>[] additionalParameterTypes) throws Exception
    {
        Class<?>[] parameterTypes = new Class[additionalParameterTypes.length + 2];
        parameterTypes[0] = String.class;
        parameterTypes[1] = int.class;
        System.arraycopy(additionalParameterTypes, 0, parameterTypes, 2, additionalParameterTypes.length);
        try {
			return newConstructorAccessorM.invoke(reflectionFactory, enumClass.getDeclaredConstructor(parameterTypes));
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
    }
	
	@Overwrite
	public static void setup()
    {
        if (isSetup)
        {
            return;
        }

        try
        {
        	setup0();
            Method getReflectionFactory = PrivillegedBridge.getMethod(PrivillegedBridge.firstClass("jdk.internal.reflect.ReflectionFactory",
            		"sun.reflect.ReflectionFactory"), "getReflectionFactory");
            reflectionFactory      = PrivillegedBridge.ALL_LOOKUP.unreflect(getReflectionFactory).asFixedArity().invoke();
            newConstructorAccessorM = PrivillegedBridge.ALL_LOOKUP.unreflect(PrivillegedBridge.getMethod(
            		PrivillegedBridge.firstClass("jdk.internal.reflect.ReflectionFactory", "sun.reflect.ReflectionFactory"), "newConstructorAccessor", Constructor.class)).asFixedArity();
            newInstanceM            = PrivillegedBridge.ALL_LOOKUP.unreflect(PrivillegedBridge.getMethod(
            		PrivillegedBridge.firstClass("jdk.internal.reflect.ConstructorAccessor", "sun.reflect.ConstructorAccessor"), "newInstance", Object[].class)).asFixedArity();
            newFieldAccessorM       = PrivillegedBridge.ALL_LOOKUP.unreflect(PrivillegedBridge.getMethod(
            		PrivillegedBridge.firstClass("jdk.internal.reflect.ReflectionFactory",
            		"sun.reflect.ReflectionFactory"), "newFieldAccessor", Field.class, boolean.class)).asFixedArity();
            fieldAccessorSetM       = PrivillegedBridge.ALL_LOOKUP.unreflect(PrivillegedBridge.getMethod(
            		PrivillegedBridge.firstClass("jdk.internal.reflect.FieldAccessor", "sun.reflect.FieldAccessor"),
            		"set", Object.class, Object.class)).asFixedArity();
        }
        catch (Throwable e)
        {
            FMLLog.log.error("Error setting up EnumHelper.", e);
        }

        isSetup = true;
    }
}