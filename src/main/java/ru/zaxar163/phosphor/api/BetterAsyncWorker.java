package ru.zaxar163.phosphor.api;

import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import net.minecraftforge.fml.common.FMLLog;

public class BetterAsyncWorker {
	private final BlockingDeque<TRunnable> tasks;
	private final List<Thread> threads;
	private final Thread[] coreThreads;
	private final Runnable otherWorkers;
	private final int maxAdd;
	private final int oneThreadLoadFactor;
	private final int addRequiredLoad;
	private final ThreadFactory thr;

	@FunctionalInterface
	public static interface TRunnable {
		void run() throws Throwable;
	}
	
	public static TRunnable asTRunnable(Runnable r) {
		return () -> r.run();
	}
	
	public BetterAsyncWorker(final BlockingDeque<TRunnable> tasks, final ThreadFactory thr,
			final int core, final int max, final long timeout, final TimeUnit unit, final int oneThreadLoadFactor) {
		this.tasks = tasks;
		this.thr = thr;
		this.coreThreads = new Thread[core];
		final Runnable coreT = () -> {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					run(this.tasks.take());
				} catch (InterruptedException e) {
					break;
				}
			}
		};
		maxAdd = max-core;
		for (int i = 0; i < core; i++)
			coreThreads[i] = start(thr.newThread(coreT));			
		this.threads = new CopyOnWriteArrayList<>();
		otherWorkers = () -> {
			TRunnable r;
			try {
				r = this.tasks.poll(timeout, unit);
			} catch (InterruptedException e) {
				threads.remove(Thread.currentThread());
				return;
			}
			while (!Thread.currentThread().isInterrupted() && r != null) {
				run(r);
				try {
					r = this.tasks.poll(timeout, unit);
				} catch (InterruptedException e) {
					break;
				}
			}
			threads.remove(Thread.currentThread());
		};
		this.oneThreadLoadFactor = oneThreadLoadFactor;
		addRequiredLoad = oneThreadLoadFactor*core;
		checkSpace();
	}
	
	private Thread start(Thread thr) {
		thr.start();
		return thr;
	}

	public void submit(TRunnable r) {
		tasks.addLast(r);
		checkSpace();
	}

	public void submitS(TRunnable r) {
		tasks.addFirst(r);
		checkSpace();
	}
	
	public void checkSpace() {
		if (threads.size() < maxAdd && tasks.size() > (addRequiredLoad+threads.size()*oneThreadLoadFactor))
			threads.add(start(thr.newThread(otherWorkers)));
	}

	public void run(TRunnable r) {
		try {
			r.run();
		} catch (Throwable e) {
			if (e instanceof RuntimeException) throw (RuntimeException) e;
			if (e instanceof Error) throw (Error) e;
			FMLLog.log.fatal("An error in worker thread: ", e);
		}
	}
}
