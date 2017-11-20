import java.util.List;
import org.checkerframework.common.value.qual.MinLen;

public class IsSubarrayEq {
    // the Interning checker correctly issues an error below, but we would like to keep this test in
    // all-systems.
    // Fenum Checker should not issue a warning.  See issue 789
    // https://github.com/typetools/checker-framework/issues/789
    @SuppressWarnings({"Interning", "fenum:return.type.incompatible"})
    public static boolean isSubarrayEq(Object @MinLen(1) [] a, List<?> sub) {
        return (sub.get(0) != a[0]);
    }
}
