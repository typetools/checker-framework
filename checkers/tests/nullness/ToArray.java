import java.util.*;
import checkers.nullness.quals.*;

public class ToArray {
    private List<@Nullable String> nullableList = new ArrayList<@Nullable String>();
    private List<@NonNull String> nonnullList = new ArrayList<@NonNull String>();

    void listToArrayObject() {
        for (@Nullable Object o : nullableList.toArray());
        for (@NonNull Object o : nullableList.toArray());

        for (@Nullable Object o : nonnullList.toArray());
        for (@NonNull Object o : nonnullList.toArray());
    }

    void listToArrayE() {
        for (@Nullable String o : nullableList.toArray(new @Nullable String[0]));
        for (@NonNull String o : nullableList.toArray(new @Nullable String[0]));
        for (@Nullable String o : nullableList.toArray(new @NonNull String[0]));
        for (@NonNull String o : nullableList.toArray(new @NonNull String[0]));

        for (@Nullable String o : nonnullList.toArray(new @Nullable String[0]));
        for (@NonNull String o : nonnullList.toArray(new @Nullable String[0]));
        for (@Nullable String o : nonnullList.toArray(new @NonNull String[0]));
        for (@NonNull String o : nonnullList.toArray(new @NonNull String[0]));
    }

    private Collection<@Nullable String> nullableCol = new ArrayList<@Nullable String>();
    private Collection<@NonNull String> nonnullCol = new ArrayList<@NonNull String>();

    void colToArrayObject() {
        for (@Nullable Object o : nullableCol.toArray());
        for (@NonNull Object o : nullableCol.toArray());

        for (@Nullable Object o : nonnullCol.toArray());
        for (@NonNull Object o : nonnullCol.toArray());
    }

    void colToArrayE() {
        for (@Nullable String o : nullableCol.toArray(new @Nullable String[0]));
        for (@NonNull String o : nullableCol.toArray(new @Nullable String[0]));
        for (@Nullable String o : nullableCol.toArray(new @NonNull String[0]));
        for (@NonNull String o : nullableCol.toArray(new @NonNull String[0]));

        for (@Nullable String o : nonnullCol.toArray(new @Nullable String[0]));
        for (@NonNull String o : nonnullCol.toArray(new @Nullable String[0]));
        for (@Nullable String o : nonnullCol.toArray(new @NonNull String[0]));
        for (@NonNull String o : nonnullCol.toArray(new @NonNull String[0]));
    }

}