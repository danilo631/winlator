package com.winlator.optimization;

import java.util.Arrays;

/**
 * Constant-space estimator for content-update intervals (NOT actual GPU present times).
 * Records no allocations in the hot update path. Statistics are computed on demand.
 */
public final class FrameTimeTracker {
    private static final long MAX_GAP_NS = 2_000_000_000L;
    private static final double NS_TO_MS = 1.0 / 1_000_000.0;
    private final long[] intervals;
    private int cursor;
    private int count;
    private long lastTimestamp = -1L;

    public FrameTimeTracker(int historySize) {
        if (historySize < 8 || historySize > 2048)
            throw new IllegalArgumentException("Invalid history size");
        intervals = new long[historySize];
    }

    public synchronized void reset() {
        cursor = 0;
        count = 0;
        lastTimestamp = -1L;
    }

    public synchronized void record(long nanoTime) {
        if (nanoTime < 0) return;
        if (lastTimestamp >= 0) {
            long delta = nanoTime - lastTimestamp;
            if (delta > 0 && delta <= MAX_GAP_NS) {
                intervals[cursor] = delta;
                cursor = (cursor + 1) % intervals.length;
                if (count < intervals.length) count++;
            } else if (delta > MAX_GAP_NS || delta < 0) {
                // A suspended or reset clock should not poison the new session.
                count = 0;
                cursor = 0;
            }
        }
        lastTimestamp = nanoTime;
    }

    public Stats snapshot() {
        long[] copy;
        synchronized (this) {
            copy = Arrays.copyOf(intervals, count);
        }
        if (copy.length == 0) return new Stats(0, 0, 0, 0);
        Arrays.sort(copy);
        long total = 0;
        for (long v : copy) total += v;

        int p95 = Math.max(0, (int) Math.ceil(copy.length * 0.95) - 1);
        int p99 = Math.max(0, (int) Math.ceil(copy.length * 0.99) - 1);
        return new Stats(copy.length, (total / (double) copy.length) * NS_TO_MS,
            copy[p95] * NS_TO_MS, copy[p99] * NS_TO_MS);
    }

    public static final class Stats {
        public final int samples;
        public final double meanMs;
        public final double p95Ms;
        public final double p99Ms;

        private Stats(int samples, double meanMs, double p95Ms, double p99Ms) {
            this.samples = samples;
            this.meanMs = meanMs;
            this.p95Ms = p95Ms;
            this.p99Ms = p99Ms;
        }
    }
}
