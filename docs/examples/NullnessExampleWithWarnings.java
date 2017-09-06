import java.util.LinkedList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.*;

/**
 * This class illustrates use of nullness type annotations. The class doesn't do anything -- it is
 * merely meant to be compiled. Compilation will produce warning messages.
 *
 * <p>There are two related files that differ only slightly: {@link NullnessExample}, an example of
 * correct use, and {@link NullnessExampleWithWarnings}, an example of incorrect use. See the
 * Nullness Checker documentation for larger examples of annotated code.
 */
public class NullnessExampleWithWarnings {

    public void example() {

        // In general, you do not have to annotate local variables, because the
        // Nullness Checker infers such annotations.  It is written here in the
        // example for emhpasis.
        @NonNull String foo = "foo";
        String bar = null;

        foo = bar;
        bar = foo;
    }

    public String exampleGenerics() {

        List<@NonNull String> foo = new LinkedList<@NonNull String>();
        List<String> bar = foo;

        String quux = null;
        foo.add(quux);
        foo.add("quux");
        @NonNull String baz = foo.get(0);
        return baz;
    }
}
