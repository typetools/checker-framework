import checkers.javari.quals.*;

public class ReadOnlyConstructor {
    int i;

    ReadOnlyConstructor() @ReadOnly {
        this.i = 3;
    }

    void test() {
        @Mutable ReadOnlyConstructor c = new ReadOnlyConstructor();
    }
}
