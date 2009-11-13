import java.util.*;
import checkers.nullness.quals.*;
// bound errors are errors related to collection not accepting nullable elements
public class ToArray {
    private List<@Nullable String> nullableList = new ArrayList<@Nullable String>(); // bound error
    private List<@NonNull String> nonnullList = new ArrayList<@NonNull String>(); // bound error

    void listToArrayObject() {
        for (@Nullable Object o : nullableList.toArray());
        for (@NonNull Object o : nullableList.toArray());   // error

        for (@Nullable Object o : nonnullList.toArray());
        for (@NonNull Object o : nonnullList.toArray());
    }

    void listToArrayE() {
        for (@Nullable String o : nullableList.toArray(new @Nullable String[0]));
        for (@NonNull String o : nullableList.toArray(new @Nullable String[0]));    // error
        for (@Nullable String o : nullableList.toArray(new @NonNull String[0]));
        for (@NonNull String o : nullableList.toArray(new @NonNull String[0]));

        for (@Nullable String o : nonnullList.toArray(new String[0]));
        for (@NonNull String o : nonnullList.toArray(new @Nullable String[0])); // error
        for (@Nullable String o : nonnullList.toArray(new @NonNull String[0]));
        for (@NonNull String o : nonnullList.toArray(new @NonNull String[0]));
    }

    private Collection<@Nullable String> nullableCol = new ArrayList<@Nullable String>();   // bound error
    private Collection<@NonNull String> nonnullCol = new ArrayList<@NonNull String>();  // bound error

    void colToArrayObject() {
        for (@Nullable Object o : nullableCol.toArray());
        for (@NonNull Object o : nullableCol.toArray());    // error

        for (@Nullable Object o : nonnullCol.toArray());
        for (@NonNull Object o : nonnullCol.toArray());
    }

    void colToArrayE() {
        for (@Nullable String o : nullableCol.toArray(new @Nullable String[0]));
        for (@NonNull String o : nullableCol.toArray(new @Nullable String[0])); // error
        for (@Nullable String o : nullableCol.toArray(new @NonNull String[0]));
        for (@NonNull String o : nullableCol.toArray(new @NonNull String[0]));  // error

        for (@Nullable String o : nonnullCol.toArray(new String[0]));
        for (@NonNull String o : nonnullCol.toArray(new @Nullable String[0]));  // error
        for (@Nullable String o : nonnullCol.toArray(new @NonNull String[0]));
        for (@NonNull String o : nonnullCol.toArray(new @NonNull String[0]));
    }

    void testHearusitics() {
        for (@Nullable String o : nonnullCol.toArray(new String[] {}));
        for (@NonNull  String o : nonnullCol.toArray(new String[] {}));
        for (@Nullable String o : nonnullCol.toArray(new String[0]));
        for (@NonNull  String o : nonnullCol.toArray(new String[0]));
        for (@Nullable String o : nonnullCol.toArray(new String[nonnullCol.size()]));
        for (@NonNull  String o : nonnullCol.toArray(new String[nonnullCol.size()]));

        for (@Nullable String o : nonnullCol.toArray(new @Nullable String[] {null}));
        for (@NonNull  String o : nonnullCol.toArray(new @Nullable String[] {null})); // error
        for (@Nullable String o : nonnullCol.toArray(new String[1]));
        for (@NonNull  String o : nonnullCol.toArray(new String[1]));   // error
        for (@Nullable String o : nonnullCol.toArray(new String[nonnullCol.size() + 1]));
        for (@NonNull  String o : nonnullCol.toArray(new String[nonnullCol.size() + 1]));   // error

        // cannot handle the following cases for now
        for (@Nullable String o : nonnullCol.toArray(new String[nonnullCol.size() - 1]));
        for (@NonNull  String o : nonnullCol.toArray(new String[nonnullCol.size() - 1]));   // error
    }
}