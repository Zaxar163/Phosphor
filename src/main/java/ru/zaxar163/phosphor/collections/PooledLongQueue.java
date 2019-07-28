package ru.zaxar163.phosphor.collections;

import java.util.ArrayDeque;
import java.util.Deque;

//Implement own queue with pooled segments to reduce allocation costs and reduce idle memory footprint
public class PooledLongQueue {
	public class LongQueueIterator {
		private Segment cur;
		private long[] curArray;

		private int index, capacity;

		private LongQueueIterator(Segment cur) {
			this.cur = cur;

			if (this.cur != null) {
				curArray = cur.longArray;
				capacity = cur.index;
			}
		}

		public void finish() {
			clear();
		}

		public boolean hasNext() {
			return cur != null;
		}

		public long next() {
			final long ret = curArray[index++];

			if (index == capacity) {
				index = 0;

				cur = cur.next;

				if (cur != null) {
					curArray = cur.longArray;
					capacity = cur.index;
				}
			}

			return ret;
		}
	}

	public static class Pool {
		private final Deque<Segment> segmentPool = new ArrayDeque<>();

		private Segment acquire() {
			if (segmentPool.isEmpty())
				return new Segment(this);

			return segmentPool.pop();
		}

		private void release(Segment segment) {
			if (segmentPool.size() < CACHED_QUEUE_SEGMENTS_COUNT)
				segmentPool.push(segment);
		}
	}

	private static class Segment {
		private final long[] longArray = new long[QUEUE_SEGMENT_SIZE];
		private int index = 0;
		private Segment next;
		private final Pool pool;

		private Segment(Pool pool) {
			this.pool = pool;
		}

		private void release() {
			index = 0;
			next = null;

			pool.release(this);
		}
	}

	private static final int CACHED_QUEUE_SEGMENTS_COUNT = 1 << 12; // 4096

	private static final int QUEUE_SEGMENT_SIZE = 1 << 10; // 1024

	private final Pool pool;

	private Segment cur, last;

	private int size = 0;

	// Stores whether or not the queue is empty. Updates to this field will be seen
	// by all threads immediately. Writes
	// to volatile fields are generally quite a bit more expensive, so we avoid
	// repeatedly setting this flag to true.
	private volatile boolean empty;

	public PooledLongQueue(Pool pool) {
		this.pool = pool;
	}

	/**
	 * Not thread-safe! Adds an encoded long value into this queue.
	 * 
	 * @param val
	 *            The encoded value to add
	 */
	public void add(final long val) {
		if (cur == null) {
			empty = false;
			cur = last = pool.acquire();
		}

		if (last.index == QUEUE_SEGMENT_SIZE) {
			Segment ret = last.next = last.pool.acquire();
			ret.longArray[ret.index++] = val;

			last = ret;
		} else
			last.longArray[last.index++] = val;

		++size;
	}

	private void clear() {
		Segment segment = cur;

		while (segment != null) {
			Segment next = segment.next;
			segment.release();
			segment = next;
		}

		size = 0;
		cur = null;
		last = null;
		empty = true;
	}

	/**
	 * Thread-safe method to check whether or not this queue has work to do.
	 * Significantly cheaper than acquiring a lock.
	 * 
	 * @return True if the queue is empty, otherwise false
	 */
	public boolean isEmpty() {
		return empty;
	}

	/**
	 * Not thread safe! Creates an iterator over the values in this queue. Values
	 * will be returned in a FIFO fashion.
	 * 
	 * @return The iterator
	 */
	public LongQueueIterator iterator() {
		return new LongQueueIterator(cur);
	}

	/**
	 * Not thread-safe! If you must know whether or not the queue is empty, please
	 * use {@link PooledLongQueue#isEmpty()}.
	 *
	 * @return The number of encoded values present in this queue
	 */
	public int size() {
		return size;
	}

}
