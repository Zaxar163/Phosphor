package ru.zaxar163.optim;

import java.util.concurrent.atomic.AtomicReference;

public class Threader {
	public static final AtomicReference<Thread> CLIENT = new AtomicReference<>(null);
	public static final AtomicReference<Thread> SERVER = new AtomicReference<>(null);
}
