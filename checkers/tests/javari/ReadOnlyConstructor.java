import checkers.javari.quals.*;

public class ReadOnlyConstructor {
    int i;

    // TODO: This test case doesn't test anything.
    // The constructor used to have an annotation, which is not legal
    // for a type annotation.
    // Also see Constructors in igj.
    ReadOnlyConstructor() {
        this.i = 3;
    }

    void test() {
        @Mutable ReadOnlyConstructor c = new ReadOnlyConstructor();
    }
}
