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

    @NonNull String foo = "foo";
    String bar = null;

    foo = bar;
    bar = foo;

  }

  public void exampleGenerics() {

    List</*@NonNull*/ String> foo = new LinkedList</*@NonNull*/ String>();
    List<String> bar = foo;

    String quux = null;
    foo.add(quux);
    foo.add("quux");
    @NonNull String baz = foo.get(0);

  }

}
