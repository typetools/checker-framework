import checkers.javari.quals.*;

public class ReadOnlyConstructor {
    int i;

    @ReadOnly ReadOnlyConstructor() {
        this.i = 3;
    }

    void test() {
        @Mutable ReadOnlyConstructor c = new ReadOnlyConstructor();
    }
}
