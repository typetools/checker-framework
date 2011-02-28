import java.util.*;
import checkers.nullness.quals.*;
// bound errors are errors related to collection not accepting nullable elements
public class ToArray {
    //:: (generic.argument.invalid) :: (generic.argument.invalid)
    private List<@Nullable String> nullableList = new ArrayList<@Nullable String>(); // bound error
    private List<@NonNull String> nonnullList = new ArrayList<@NonNull String>(); // bound error

    void listToArrayObject() {
        for (@Nullable Object o : nullableList.toArray());
        //:: (enhancedfor.type.incompatible)
        for (@NonNull Object o : nullableList.toArray());   // error

        for (@Nullable Object o : nonnullList.toArray());
        for (@NonNull Object o : nonnullList.toArray());
    }

    void listToArrayE() {
        for (@Nullable String o : nullableList.toArray(new @Nullable String[0]));
        //:: (enhancedfor.type.incompatible)
        for (@NonNull String o : nullableList.toArray(new @Nullable String[0]));    // error
        for (@Nullable String o : nullableList.toArray(new @NonNull String[0]));
        //:: (enhancedfor.type.incompatible)
        for (@NonNull String o : nullableList.toArray(new @NonNull String[0]));

        for (@Nullable String o : nonnullList.toArray(new String[0]));
        //TODO: Expected error not found!
        //TODO:: (argument.type.incompatible)
        for (@NonNull String o : nonnullList.toArray(new @Nullable String[0])); // error
        for (@Nullable String o : nonnullList.toArray(new @NonNull String[0]));
        for (@NonNull String o : nonnullList.toArray(new @NonNull String[0]));
    }

    //:: (generic.argument.invalid)
    private Collection<@Nullable String> nullableCol = new ArrayList<@Nullable String>();   // bound error
    private Collection<@NonNull String> nonnullCol = new ArrayList<@NonNull String>();  // bound error

    void colToArrayObject() {
        for (@Nullable Object o : nullableCol.toArray());
        //:: (enhancedfor.type.incompatible)
        for (@NonNull Object o : nullableCol.toArray());    // error

        for (@Nullable Object o : nonnullCol.toArray());
        for (@NonNull Object o : nonnullCol.toArray());
    }

    void colToArrayE() {
        for (@Nullable String o : nullableCol.toArray(new @Nullable String[0]));
        //:: (enhancedfor.type.incompatible)
        for (@NonNull String o : nullableCol.toArray(new @Nullable String[0])); // error
        for (@Nullable String o : nullableCol.toArray(new @NonNull String[0]));
        //:: (enhancedfor.type.incompatible)
        for (@NonNull String o : nullableCol.toArray(new @NonNull String[0]));  // error

        for (@Nullable String o : nonnullCol.toArray(new String[0]));
        //TODO: Expected error not found!
        //TODO:: (argument.type.incompatible)
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
        //:: (enhancedfor.type.incompatible)
        for (@NonNull  String o : nonnullCol.toArray(new @Nullable String[] {null})); // error
        for (@Nullable String o : nonnullCol.toArray(new String[1]));
        //:: (enhancedfor.type.incompatible)
        for (@NonNull  String o : nonnullCol.toArray(new String[1]));   // error
        for (@Nullable String o : nonnullCol.toArray(new String[nonnullCol.size() + 1]));
        //:: (enhancedfor.type.incompatible)
        for (@NonNull  String o : nonnullCol.toArray(new String[nonnullCol.size() + 1]));   // error

        // cannot handle the following cases for now
        for (@Nullable String o : nonnullCol.toArray(new String[nonnullCol.size() - 1]));
        //:: (enhancedfor.type.incompatible)
        for (@NonNull  String o : nonnullCol.toArray(new String[nonnullCol.size() - 1]));   // error
    }
}