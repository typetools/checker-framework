// Test case for Issue 1027:
// https://github.com/typetools/checker-framework/issues/1027

// Use  -J-XX:MaxJavaStackTraceDepth=1000000 as parameter
// to javac to see a longer stacktrace.

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.KeyFor;

public class Issue1027 {

    // Stand-alone reproduction

    class Repr<T> {
        void bar(Function<T, String> p) {}
    }

    @SuppressWarnings("nullness")
    Repr<@KeyFor("this") String> foo() {
        return null;
    }

    void zoo(Issue1027 p) {
        p.foo().bar(x -> "");
    }

    // Various longer versions that also used to give SOE

    void foo(Map<String, String> arg) {
        arg.keySet().stream().map(key -> key);
    }

    Stream<String> foo(Set<String> arg) {
        return arg.stream().map(key -> key);
    }

    String foo(Stream<String> stream) {
        return stream.map(key -> key).collect(Collectors.joining());
    }
}
