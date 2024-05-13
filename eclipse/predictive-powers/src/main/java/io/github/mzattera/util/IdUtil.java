/**
 * 
 */
package io.github.mzattera.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates unique long IDs.
 * IDs are guaranteed to be unique inside the JVM.ight not be unique over subsequent runs.
 */
public final class IdUtil {

	private static final AtomicLong counter = new AtomicLong(System.nanoTime());

	private IdUtil() {
	}

	public static long getLongId() {
		return counter.getAndIncrement();
	}

	public static String getStringId() {
		return Long.toString(getLongId());
	}
}