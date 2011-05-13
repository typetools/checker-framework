import checkers.nullness.quals.*;

public class FlowAssignment {

    void test() {
        @NonNull String s = "foo";

        String t = s;
        t.startsWith("f");
    }

}
