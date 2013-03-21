/*
 * @test
 * @summary Test that the stub files get invoked
 * @library .
 * @compile -processor checkers.nullness.NullnessChecker -Astubs=sample.astub Sample.java
 */

import java.util.*;

class Sample {
    void test() {
        Object o = null;
        String v = String.valueOf(o);
    }
}
