import java.util.LinkedList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.*;

/**
 * This class is based on NullnessExample. This version contains additional tests to ensure that a
 * build works correctly.
 */
public class NullnessReleaseTests {

    public void example() {
        @NonNull String foo = "foo";
        @NonNull String bar = "bar";

        foo = bar;
        bar = foo;
    }

    public @NonNull String exampleGenerics() {
        List<@NonNull String> foo = new LinkedList<@NonNull String>();
        List<@NonNull String> bar = foo;

        @NonNull String quux = "quux";
        foo.add(quux);
        foo.add("quux");
        @NonNull String baz = foo.get(0);
        return baz;
    }

    // For some reason this class causes an exception if the Checker
    // Framework is compiled with JDK 7 and then executed on JDK 6.
    class TestException extends Exception {}
}
