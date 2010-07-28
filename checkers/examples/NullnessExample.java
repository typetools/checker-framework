import checkers.nullness.quals.*;
import java.util.*;

/**
 * This class illustrates a correct use of the @NonNull type annotation.
 * The class doesn't do anything -- it is merely meant to be compiled.
 * Compilation will produce no warning messages.
 * <p>
 *
 * Also see {@link NullnessExampleWithWarnings}, an example of incorrect use
 * of the @NonNull type annotation.  See the Nullness checker documentation
 * for larger examples of annotated code.
 **/
public class NullnessExample {

  public void example() {

    // These type annotations are redundant -- the Nullness Checker will
    // infer them, but they are written here in the example for emhpasis.
    // In general, you do not have to annotate local variables.
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
