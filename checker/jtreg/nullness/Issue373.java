/*
 * @test
 * @summary Test for Issue 373: message duplicated when using -Awarns
 *
 * @compile/fail/ref=Issue373-err.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Alint Issue373.java
 * @compile/ref=Issue373-warn.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Alint Issue373.java -Awarns
 */

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class Issue373 extends AbstractMap<String, String> {
    @Override
    public Set<Map.Entry<String, String>> entrySet() {
        return Collections.emptySet();
    }
}
