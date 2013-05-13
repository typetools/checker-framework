import checkers.nullness.quals.*;
import java.util.*;

/**
 * This class illustrates use of nullness type annotations.
 * The class doesn't do anything -- it is merely meant to be compiled.
 * Compilation will produce no warning messages.
 * <p>
 *
 * There are two related files that differ only slightly:
 * {@link NullnessExample}, an example of correct use, and {@link
 * NullnessExampleWithWarnings}, an example of incorrect use.
 * See the Nullness checker documentation for larger examples of annotated code.
 **/
public class NullnessExample {

  public void example() {

    // In general, you do not have to annotate local variables, because the
    // Nullness Checker infers such annotations.  It is written here in the
    // example for emhpasis.
    @NonNull String foo = "foo";
    @NonNull String bar = "bar";

    foo = bar;
    bar = foo;

  }

  public @NonNull String exampleGenerics() {

    List</*@NonNull*/ String> foo = new LinkedList</*@NonNull*/ String>();
    List</*@NonNull*/ String> bar = foo;

    @NonNull String quux = "quux";
    foo.add(quux);
    foo.add("quux");
    @NonNull String baz = foo.get(0);
    return baz;

  }

}
