// Test case for issue #2358: https://tinyurl.com/cfissue/#2358

// @skip-test until the bug is fixed.

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.KeyFor;

public class KeyForMultiple {

    void m1() {

        Map<@KeyFor({"sharedBooks"}) String, Integer> sharedBooks = new HashMap<>();

        Map<@KeyFor({"sharedBooks"}) String, Integer> sharedCounts1 = new HashMap<>();
        Set<@KeyFor({"sharedCounts1"}) String> sharedCountsKeys1 = sharedCounts1.keySet();
    }

    void m2() {

        Map<@KeyFor({"sharedBooks"}) String, Integer> sharedBooks = new HashMap<>();

        Map<@KeyFor({"sharedBooks"}) String, Integer> sharedCounts1 = new HashMap<>();
        Set<@KeyFor({"sharedBooks", "sharedCounts1"}) String> otherChars1 = sharedCounts1.keySet();
    }

    void m3() {

        Map<@KeyFor({"sharedBooks"}) String, Integer> sharedBooks = new HashMap<>();

        Map<@KeyFor({"sharedBooks", "sharedCounts2"}) String, Integer> sharedCounts2 =
                new HashMap<>();
        Set<@KeyFor({"sharedCounts2"}) String> sharedCountsKeys2 = sharedCounts2.keySet();
    }

    void m4() {

        Map<@KeyFor({"sharedBooks"}) String, Integer> sharedBooks = new HashMap<>();

        Map<@KeyFor({"sharedBooks", "sharedCounts2"}) String, Integer> sharedCounts2 =
                new HashMap<>();
        Set<@KeyFor({"sharedBooks", "sharedCounts2"}) String> otherChars2 = sharedCounts2.keySet();
    }
}
