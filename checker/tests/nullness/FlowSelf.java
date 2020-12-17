import org.checkerframework.checker.nullness.qual.*;

public class FlowSelf {

    void test(@Nullable String s) {

        if (s == null) {
            return;
        }
        // :: warning: (known.nonnull)
        assert s != null;

        s = s.substring(1);
    }
}
