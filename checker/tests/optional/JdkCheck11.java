// @below-java11-jdk-skip-test

import java.util.Optional;
import org.checkerframework.checker.optional.qual.Present;

/** Test JDK annotations, for methods added after JDK 8. */
@SuppressWarnings("optional.parameter")
public class JdkCheck11 {

    String isEmptyTest1(Optional<String> pos, String fallback) {
        if (pos.isEmpty()) {
            return fallback;
        }
        return pos.get();
    }

    String orElseThrowTest1(@Present Optional<String> pos) {
        return pos.orElseThrow();
    }

    String orElseThrowTest2(Optional<String> mos) {
        // :: error: (method.invocation.invalid)
        return mos.orElseThrow();
    }

    String orElseThrowTestFlow(Optional<String> mos) {
        // :: error: (method.invocation.invalid)
        mos.orElseThrow();
        return mos.get();
    }
}
