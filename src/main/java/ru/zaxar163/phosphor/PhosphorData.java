package ru.zaxar163.phosphor;

import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.util.IProgressUpdate;

public class PhosphorData {
	public static final AtomicReference<Thread> CLIENT = new AtomicReference<>(null);
	public static IProgressUpdate PROGRESS_SAVE_WORLDS = null;
}
