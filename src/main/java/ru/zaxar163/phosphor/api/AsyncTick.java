package ru.zaxar163.phosphor.api;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.NavigableMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import ru.zaxar163.phosphor.PhosphorData;
import ru.zaxar163.phosphor.api.BetterAsyncWorker.TRunnable;
import ru.zaxar163.phosphor.mixins.plugins.OptimEnginePlugin;

public class AsyncTick {
	public static final AsyncTick INSTANCE = new AsyncTick();
	public static final MethodHandle treeSetConstructor;
	static {
		try {
			treeSetConstructor = PrivillegedBridge.ALL_LOOKUP.findConstructor(TreeSet.class,
					MethodType.methodType(void.class, NavigableMap.class));
		} catch (Throwable e) {
			throw new Error(e);
		}
	}
	private BetterAsyncWorker threadPool;

    public static <T> TreeSet<T> newTreeSet() {
		try {
			return (TreeSet<T>) AsyncTick.treeSetConstructor.invoke(new ConcurrentSkipListMap<T, Object>());
		} catch (Throwable e) {
			throw new Error(e); // never happen
		}
    }

    public void run(TRunnable r) {
		threadPool.submit(r);
	}
	
	public void forceRun(TRunnable r) {
		threadPool.submitS(r);
	}

	public void finishTick() {
	}
	
	private AsyncTick() {
		final ThreadGroup gr = PhosphorData.SERVER.get().getThreadGroup();
		threadPool = new BetterAsyncWorker(new LinkedBlockingDeque<>(), gr instanceof ThreadFactory ? (ThreadFactory)gr : e -> new Thread(gr, e, "Ticker"),
				OptimEnginePlugin.CFG.poolThreads,
				OptimEnginePlugin.CFG.maxThreads, 800L, TimeUnit.MILLISECONDS, 10);
	}
}
