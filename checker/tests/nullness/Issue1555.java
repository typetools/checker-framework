import org.checkerframework.checker.nullness.NullnessUtil;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

class Issue1555 {

    @MonotonicNonNull private String x;

    String test() {
        return NullnessUtil.castNonNull(x);
    }
}
