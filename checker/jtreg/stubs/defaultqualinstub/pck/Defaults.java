package pck;

public class Defaults {
    Object o;

    void test() {
        // The astub file changes o to @Nullable
        // and therefore the assignment is allowed.
        o = null;
    }
}
