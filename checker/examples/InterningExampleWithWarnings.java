import org.checkerframework.checker.interning.qual.*;

/**
 * This class illustrates an incorrect use of the @{@link Interned} type annotation. The class
 * doesn't do anything -- it is merely meant to be compiled. Compilation will produce warning
 * messages.
 *
 * <p>Also see {@link InterningExample}, an example of correct use of the @Interned type annotation.
 * See the Interning Checker documentation for larger examples of annotated code.
 */
public class InterningExampleWithWarnings {

    public void example() {

        // This type annotation is redundant -- the Interning Checker will
        // infer it, but it is written here in the example for emhpasis.
        // In general, you do not have to annotate local variables.
        @Interned String foo = "foo";
        String bar = new String("bar");

        if (foo == bar) {
            System.out.println("foo == bar");
        }
    }
}
