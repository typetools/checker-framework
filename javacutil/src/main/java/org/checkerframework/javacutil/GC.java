package org.checkerframework.javacutil;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// TODO: Move this to the UtilPlume class.
/** Utilities related to garbage collection. */
public class GC {

    /**
     * A list of pairs of (timestamp, accumulated collection time). The timestamp is an epoch
     * second, and the collection time is in milliseconds. New items are added to the front.
     */
    private static List<Pair<Long, Long>> gcHistory = new ArrayList<>();

    /**
     * Returns the percentage of time spent garbage collecting, in the past minute. Might return a
     * value greater than 1 if multiple threads are spending all their time collecting. Returns 0 if
     * {@code gcPercentage} was not first called more than 1 minute ago.
     *
     * <p>A typical use is to put the following in an outer loop that takes a significant amount of
     * time (more than a second) to execute:
     *
     * <pre>{@code
     * if (GC.gcPercentage() > .25) {
     *   throw new Error("The JVM has too little memory; re-run with larger max heap.");
     * }
     * }</pre>
     *
     * @return the percentage of time spent garbage collecting, in the past minute
     */
    public static float gcPercentage() {
        return gcPercentage(60);
    }

    /**
     * Returns the percentage of time spent garbage collecting, in the past {@code seconds} seconds.
     * Might return a value greater than 1 if multiple threads are spending all their time
     * collecting. Returns 0 if {@code gcPercentage} was not first called more than {@code seconds}
     * seconds ago.
     *
     * <p>A typical use is to put the following in an outer loop that takes a significant amount of
     * time (more than a second) to execute:
     *
     * <pre>{@code
     * if (GC.gcPercentage(10) > .25) {
     *   throw new Error("The JVM has too little memory; re-run with larger max heap.");
     * }
     * }</pre>
     *
     * @param seconds the size of the time window, in seconds
     * @return the percentage of time spent garbage collecting, in the past {@code seconds} seconds
     */
    public static float gcPercentage(int seconds) {
        long now = Instant.now().getEpochSecond();
        long collectionTime = getCollectionTime();
        gcHistory.add(Pair.of(now, collectionTime));

        for (Pair<Long, Long> p : gcHistory) {
            if (now - p.first >= seconds) {
                return (float) ((collectionTime - p.second) / 1000.0 / (now - p.first));
            }
        }
        return 0;
    }

    /**
     * Return the accumulated garbage collection time in milliseconds.
     *
     * @return the accumulated garbage collection time in milliseconds
     */
    private static long getCollectionTime() {
        long result = 0;
        for (GarbageCollectorMXBean b : ManagementFactory.getGarbageCollectorMXBeans()) {
            long time = b.getCollectionTime();
            if (time != -1) {
                result += time;
            }
        }
        return result;
    }
}
