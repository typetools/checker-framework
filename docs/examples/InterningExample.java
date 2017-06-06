import org.checkerframework.checker.interning.qual.*;

/**
 * This class illustrates a correct use of the @{@link Interned} type annotation. The class doesn't
 * do anything -- it is merely meant to be compiled. Compilation will produce no warning messages.
 *
 * <p>Also see {@link InterningExampleWithWarnings}, an example of incorrect use of the Interned
 * type annotation. See the Interning Checker documentation for larger examples of annotated code.
 */
public class InterningExample {

    public void example() {

        // These type annotations are redundant -- the Interning Checker will
        // infer them, but they are written here in the example for emhpasis.
        // In general, you do not have to annotate local variables.
        @Interned String foo = "foo";
        @Interned String bar = "bar";

        if (foo == bar) {
            System.out.println("foo == bar");
        }
    }
}
