import checkers.nullness.quals.*;
import java.util.*;

/**
 * This class illustrates a correct use of the @NonNull type annotation.
 * The class doesn't do anything -- it is merely meant to be compiled.
 * Compilation will produce no warning messages.
 * <p>
 *
 * Also see {@link NullnessExampleWithWarnings}, an example of incorrect use
 * of the NonNull type annotation.  See the Nullness checker documentation
 * for larger examples of annotated code.
 **/
public class NullnessExample {

  public void example() {

    @NonNull String foo = "foo";
    @NonNull String bar = "bar";

    foo = bar;
    bar = foo;

  }

  public void exampleGenerics() {

    List</*@NonNull*/ String> foo = new LinkedList</*@NonNull*/ String>();
    List</*@NonNull*/ String> bar = foo;

    @NonNull String quux = "quux";
    foo.add(quux);
    foo.add("quux");
    @NonNull String baz = foo.get(0);

  }

}
