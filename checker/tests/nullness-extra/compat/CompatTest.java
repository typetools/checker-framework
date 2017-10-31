import lib.Lib;
import org.checkerframework.checker.nullness.qual.NonNull;

public class CompatTest {
    void m() {
        @NonNull Object o = Lib.maybeGetObject();
    }
}
