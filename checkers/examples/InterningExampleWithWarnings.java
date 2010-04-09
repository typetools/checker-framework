import checkers.interning.quals.*;
import java.util.*;

/**
 * This class illustrates an incorrect use of the @{@link Interned} type annotation.
 * The class doesn't do anything -- it is merely meant to be compiled.
 * Compilation will produce warning messages.
 * <p>

 * Also see {@link InterningExample}, an example of correct use
 * of the @Interned type annotation.  See the Interning checker documentation
 * for larger examples of annotated code.
 **/
public class InterningExampleWithWarnings {

  public void example() {

    @Interned String foo = "foo";
    String bar = new String("bar");

    if (foo == bar)
        System.out.println("foo == bar");

  }

}
