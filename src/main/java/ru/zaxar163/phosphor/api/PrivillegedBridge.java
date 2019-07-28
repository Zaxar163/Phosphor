package ru.zaxar163.phosphor.api;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public final class PrivillegedBridge {
	private PrivillegedBridge() {}
	private static final class ClassData {

		private static final ClassValue<ClassData> VAL = new ClassValue<ClassData>() {
			@Override
			protected ClassData computeValue(final Class<?> type) {
				return new ClassData(type);
			}
		};
		@SuppressWarnings("rawtypes")
		private final Constructor[] constructors;
		private final Field[] fields;
		private final Method[] methods;

		private ClassData(final Class<?> clazz) {
			try {
				constructors = (Constructor[]) CONSTRUCTORS_GETTER.invokeExact(clazz, false);
				methods = (Method[]) METHODS_GETTER.invokeExact(clazz, false);
				fields = (Field[]) FIELDS_GETTER.invokeExact(clazz, false);
			} catch (final Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static final Lookup ALL_LOOKUP;
	public static final int ALL_MODES = Lookup.PUBLIC | Lookup.PRIVATE | Lookup.PROTECTED | Lookup.PACKAGE;
	private static final MethodHandle CLASSLOADER_GETTER;
	private static final MethodHandle CONSTRUCTORS_GETTER;
	private static final MethodHandle DECLAREDCLASSES_GETTER;
	private static final MethodHandle FIELDS_GETTER;
	public static final boolean JAVA9;
	private static final MethodHandle LOOKUP_CONSTRUCTOR;
	private static final MethodHandle METHODS_GETTER;

	static {
		try {
			boolean java9 = false;
			try {
				Class.forName("java.lang.StackWalker");
				java9 = true;
			} catch (final Throwable e) {
			}
			JAVA9 = java9;
			MethodHandles.publicLookup(); // hack to cause classloading of Lookup
			final Field allPermsLookup = Arrays.stream(Lookup.class.getDeclaredFields())
					.filter(e -> e.getType().equals(Lookup.class)
							&& e.getName().toLowerCase(Locale.US).contains("lookup")
							&& e.getName().toLowerCase(Locale.US).contains("impl")
							&& !e.getName().toLowerCase(Locale.US).contains("public"))
					.findFirst().get();
			ALL_LOOKUP = JAVA9 ? DataUtil.newGet(allPermsLookup) : DataUtil.oldGet(allPermsLookup);
			LOOKUP_CONSTRUCTOR = ALL_LOOKUP
					.findVirtual(Lookup.class, "in", MethodType.methodType(Lookup.class, Class.class))
					.bindTo(ALL_LOOKUP);
			FIELDS_GETTER = ALL_LOOKUP.findSpecial(Class.class, "getDeclaredFields0",
					MethodType.methodType(Field[].class, boolean.class), Class.class);
			METHODS_GETTER = ALL_LOOKUP.findSpecial(Class.class, "getDeclaredMethods0",
					MethodType.methodType(Method[].class, boolean.class), Class.class);
			CONSTRUCTORS_GETTER = ALL_LOOKUP.findSpecial(Class.class, "getDeclaredConstructors0",
					MethodType.methodType(Constructor[].class, boolean.class), Class.class);
			DECLAREDCLASSES_GETTER = ALL_LOOKUP.findSpecial(Class.class, "getDeclaredClasses0",
					MethodType.methodType(Class[].class), Class.class);
			CLASSLOADER_GETTER = ALL_LOOKUP.findSpecial(Class.class, "getClassLoader0",
					MethodType.methodType(ClassLoader.class), Class.class);
		} catch (final Throwable e) {
			throw new Error(e);
		}
	}

	private static final class DataUtil {
		private DataUtil() { }
		private static Method a(final Method m) {
			if (!m.isAccessible())
				m.setAccessible(true);
			return m;
		}

		public static Lookup newGet(final Field lookup) throws Throwable {
			try {
				final Class<?> unsafe = Class.forName("sun.misc.Unsafe");
				final Field unsafeInst = Arrays.stream(unsafe.getDeclaredFields())
						.filter(e -> e.getType().equals(unsafe) && e.getName().toLowerCase(Locale.US).contains("unsafe"))
						.findFirst().get();
				unsafeInst.setAccessible(true);
				final Object inst = unsafeInst.get(null);
				return (Lookup) a(unsafe.getDeclaredMethod("getObject", Object.class, long.class)).invoke(inst,
						a(unsafe.getDeclaredMethod("staticFieldBase", Field.class)).invoke(inst, lookup),
						(long) a(unsafe.getDeclaredMethod("staticFieldOffset", Field.class)).invoke(inst, lookup));
			} catch (final Throwable e) {
				try {
					return oldGet(lookup);
				} catch (final Throwable t) {
					e.addSuppressed(t);
					throw e;
				}
			}
		}

		public static Lookup oldGet(final Field lookup) throws Throwable {
			AccessibleObject.setAccessible(new AccessibleObject[] { lookup }, true);
			return (Lookup) MethodHandles.publicLookup().unreflectGetter(lookup).invoke();
		}
	}
	
    public static Class<?> firstClass(final ClassLoader cl, final String... search) {
		for (final String name : search)
			try {
				return Class.forName(name, false, cl);
			} catch (final ClassNotFoundException ignored) {
				// Expected
			}
		throw new RuntimeException(new ClassNotFoundException(Arrays.toString(search)));
	}
    
    public static Class<?> firstClass(final String... search) {
    	return firstClass(PrivillegedBridge.class.getClassLoader(), search);
    }
	
	public static List<Field> digFields(final Class<?> top) {
		final List<Field> ret = new ArrayList<>();
		Class<?> superc = top;
		while (superc != null && !superc.equals(Object.class)) {
			for (final Field field : getDeclaredFields(superc))
				ret.add(field);
			superc = superc.getSuperclass();
		}
		return ret;
	}

	public static List<Method> digMethods(final Class<?> top) {
		final List<Method> ret = new ArrayList<>();
		Class<?> superc = top;
		while (superc != null && !superc.equals(Object.class)) {
			for (final Method field : getDeclaredMethods(superc))
				ret.add(field);
			superc = superc.getSuperclass();
		}
		return ret;
	}

	public static MethodHandle fromWrapped(final Object handle) {
		return MethodHandleProxies.wrapperInstanceTarget(handle);
	}

	public static ClassLoader getClassLoader(final Class<?> clazz) {
		try {
			return (ClassLoader) CLASSLOADER_GETTER.invokeExact(clazz);
		} catch (final Throwable e) {
			throw new Error(e);
		}
	}

	public static Constructor<?> getConstructor(final Class<?> cls, final Class<?>... types) {
		return Arrays.stream(getDeclaredConstructors(cls)).filter(e -> Arrays.equals(e.getParameterTypes(), types))
				.findFirst().get();
	}

	public static Class<?>[] getDeclaredClasses(final Class<?> clazz) {
		try {
			return (Class<?>[]) DECLAREDCLASSES_GETTER.invokeExact(clazz);
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> Constructor<T>[] getDeclaredConstructors(final Class<T> clazz) {
		return ClassData.VAL.get(clazz).constructors;
	}

	public static <T> Constructor<T>[] getDeclaredConstructorsNonCache(final Class<T> clazz) {
		try {
			return (Constructor<T>[]) CONSTRUCTORS_GETTER.invokeExact(clazz, false);
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static Field[] getDeclaredFields(final Class<?> clazz) {
		return ClassData.VAL.get(clazz).fields;
	}

	public static Field[] getDeclaredFieldsNonCache(final Class<?> clazz) {
		try {
			return (Field[]) FIELDS_GETTER.invokeExact(clazz, false);
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static Method[] getDeclaredMethods(final Class<?> clazz) {
		return ClassData.VAL.get(clazz).methods;
	}

	public static Method[] getDeclaredMethodsNonCache(final Class<?> clazz) {
		try {
			return (Method[]) METHODS_GETTER.invokeExact(clazz, false);
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static Field getField(final Class<?> clazz, final String name) {
		Objects.requireNonNull(name, "name");
		return Arrays.stream(getDeclaredFields(clazz)).filter(e -> name.equals(e.getName())).findFirst().get();
	}

	public static Field getField(final Class<?> clazz, final String name, final Class<?> type) {
		Objects.requireNonNull(name, "name");
		return Arrays.stream(getDeclaredFields(clazz)).filter(e -> name.equals(e.getName()) && e.getType().equals(type))
				.findFirst().get();
	}

	public static Method getMethod(final Class<?> cls, final String name, final Class<?>... types) {
		return Arrays.stream(getDeclaredMethods(cls)).filter(e -> name.equals(e.getName()))
				.filter(e -> Arrays.equals(e.getParameterTypes(), types)).findFirst().get();
	}

	public static Class<?> ifaceFromWrapped(final Object handle) {
		return MethodHandleProxies.wrapperInstanceType(handle);
	}

	public static Lookup in(final Class<?> clazz) {
		try {
			return (Lookup) LOOKUP_CONSTRUCTOR.invokeExact(clazz);
		} catch (final Throwable e) {
			throw new Error(e);
		}
	}

	public static boolean isWrapped(final Object handle) {
		return MethodHandleProxies.isWrapperInstance(handle);
	}

	public static <T> T wrap(final Class<T> iFace, final MethodHandle handle) {
		return MethodHandleProxies.asInterfaceInstance(iFace, handle);
	}
	
	public static class TraceSecurityManager extends SecurityManager {
		public Class<?>[] getClassContext() {
			return super.getClassContext();
		}
		private TraceSecurityManager() { }
		public static final TraceSecurityManager INSTANCE = new TraceSecurityManager();
	}
}
