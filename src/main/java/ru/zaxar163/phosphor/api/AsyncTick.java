package ru.zaxar163.phosphor.api;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.NavigableMap;
import java.util.TreeSet;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.minecraftforge.fml.common.FMLCommonHandler;
import ru.zaxar163.phosphor.PhosphorData;
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
	private ThreadPoolExecutor threadPool;
	private BlockingDeque<Runnable> queue;
	private BlockingDeque<Future<?>> queuer;
	public Future<?> run(Runnable r) {
		return threadPool.submit(r);
	}
	
	public Future<?> runS(Runnable r) {
		Future<?> ret = threadPool.submit(r);
		queuer.add(ret);
		return ret;
	}
	
	public Future<?> forceRun(Runnable r) {
		FutureTask<?> t = new FutureTask<>(r, null);
		queue.addFirst(t);
		return t;
	}

	public Future<?> forceRunS(Runnable r) {
		FutureTask<?> t = new FutureTask<>(r, null);
		queue.addFirst(t);
		queuer.addFirst(t);
		return t;
	}

	public void finishA() {
		for (Future<?> f : queuer) {
			try {
				f.get();
			} catch (InterruptedException e) {
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void finishTick() {
		try {
			threadPool.awaitTermination(20L, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) { }
		queuer.clear();
	}
	
	private AsyncTick() {
		FMLCommonHandler.instance().getEffectiveSide();
		final ThreadGroup gr = PhosphorData.SERVER.get().getThreadGroup();
		queue = new LinkedBlockingDeque<>();
		queuer = new LinkedBlockingDeque<>();
		threadPool = new ThreadPoolExecutor(OptimEnginePlugin.CFG.poolThreads, OptimEnginePlugin.CFG.maxThreads, 800, TimeUnit.MILLISECONDS, queue,
				gr instanceof ThreadFactory ? (ThreadFactory)PhosphorData.SERVER.get().getThreadGroup() : e -> new Thread(gr, e, "Ticker"));
	}
}
