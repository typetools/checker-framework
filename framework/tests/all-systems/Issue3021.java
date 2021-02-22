// Test case for Issue 3021:
// https://github.com/typetools/checker-framework/issues/3021

// Any arbitrary annotation can be used.
import org.checkerframework.common.aliasing.qual.MaybeAliased;

public class Issue3021 {
    <T> void make() {
        new Lib<@MaybeAliased T>() {};
    }

    class Lib<T> {}
}
