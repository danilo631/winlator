import com.winlator.optimization.FrameTimeTracker;
import com.winlator.optimization.ConfigFilePolicy;

public final class FrameTimeTrackerTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void close(double a, double b, double tolerance) {
        check(Math.abs(a - b) < tolerance, a + " not close to " + b);
    }

    public static void main(String[] args) {
        FrameTimeTracker tracker = new FrameTimeTracker(128);
        check(tracker.snapshot().samples == 0, "fresh samples");
        long ns = 1_000_000_000L;
        tracker.record(ns);
        for (int i = 0; i < 100; i++) {
            ns += 16_666_667L;
            tracker.record(ns);
        }
        FrameTimeTracker.Stats base = tracker.snapshot();
        check(base.samples == 100, "100 samples");
        close(base.p95Ms, 16.666667, 0.001);
        close(base.meanMs, 16.666667, 0.001);

        ns += 120_000_000L;
        tracker.record(ns);
        FrameTimeTracker.Stats oneStutter = tracker.snapshot();
        check(oneStutter.p99Ms > 100, "p99 must catch rare stutter");
        check(oneStutter.p95Ms < 17, "p95 stable after rare stutter");

        for (int i = 0; i < 200; i++) {
            ns += 20_000_000L;
            tracker.record(ns);
        }
        check(tracker.snapshot().samples == 128, "ring buffer bound");
        close(tracker.snapshot().p95Ms, 20, 0.001);

        ns += 4_000_000_000L;
        tracker.record(ns);
        check(tracker.snapshot().samples == 0, "suspend resets history");
        tracker.reset();
        check(tracker.snapshot().samples == 0, "explicit reset");

        check(!ConfigFilePolicy.needsWrite(true, "test\n", "test\n"), "identical config");
        check(ConfigFilePolicy.needsWrite(true, "test\n", "new\n"), "changed config");
        check(ConfigFilePolicy.needsWrite(false, "", ""), "new config");
        System.out.println("FrameTimeTrackerTest: PASS");
    }
}
