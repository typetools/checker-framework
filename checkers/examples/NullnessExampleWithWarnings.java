import checkers.nullness.quals.*;
import java.util.*;

/**
 * This class illustrates an incorrect use of the @NonNull type annotation.
 * The class doesn't do anything -- it is merely meant to be compiled.
 * Compilation will produce warning messages.
 * <p>

 * Also see {@link NullnessExample}, an example of correct use
 * of the @NonNull type annotation.  See the Nullness checker documentation
 * for larger examples of annotated code.
 **/
public class NullnessExampleWithWarnings {

  public void example() {

    // This type annotation is redundant -- the Nullness Checker will
    // infer it, but it is written here in the example for emhpasis.
    // In general, you do not have to annotate local variables.
    @NonNull String foo = "foo";
    String bar = null;

    foo = bar;
    bar = foo;

  }

  public /*@NonNull*/ String exampleGenerics() {

    List</*@NonNull*/ String> foo = new LinkedList</*@NonNull*/ String>();
    List<String> bar = foo;

    String quux = null;
    foo.add(quux);
    foo.add("quux");
    @NonNull String baz = foo.get(0);
    return baz;

  }

}
