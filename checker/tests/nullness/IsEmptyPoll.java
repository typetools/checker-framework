// Test case for Issue 399:
// https://github.com/typetools/checker-framework/issues/399

// @skip-test until the issue is fixed

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Queue;

public final class IsEmptyPoll extends ArrayList<String> {

    void mNonNull(Queue<String> q) {
        while (!q.isEmpty()) {
            @NonNull String firstNode = q.poll();
        }
    }

    void mNullable(Queue<@Nullable String> q) {
        while (!q.isEmpty()) {
            // :: error: (assignment.type.incompatible)
            @NonNull String firstNode = q.poll();
        }
    }

    void mNoCheck(Queue<@Nullable String> q) {
        // :: error: (assignment.type.incompatible)
        @NonNull String firstNode = q.poll();
    }
}
