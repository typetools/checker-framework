import org.checkerframework.checker.nullness.qual.*;

// Allow searching through nullable arrays components
// and for nullable keys.
public class BinarySearch {
    @Nullable Object @NonNull [] arr = {"a", "b", null};

    void search(@Nullable Object key) {
        int res = java.util.Arrays.binarySearch(arr, key);
        res = java.util.Arrays.binarySearch(arr, 0, 4, key);
    }
}
