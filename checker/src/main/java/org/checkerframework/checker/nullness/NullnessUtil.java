package org.checkerframework.checker.nullness;

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility class for the Nullness Checker.
 *
 * <p>To avoid the need to write the NullnessUtil class name, do:
 *
 * <pre>import static org.checkerframework.checker.nullness.NullnessUtil.castNonNull;</pre>
 *
 * or
 *
 * <pre>import static org.checkerframework.checker.nullness.NullnessUtil.*;</pre>
 *
 * <p><b>Runtime Dependency</b>
 *
 * <p>Please note that using this class introduces a runtime dependency. This means that you need to
 * distribute (or link to) the Checker Framework, along with your binaries.
 *
 * <p>To eliminate this dependency, you can simply copy this class into your own project.
 */
@SuppressWarnings({
    "nullness", // Nullness utilities are trusted regarding nullness.
    "cast" // Casts look redundant if Nullness Checker is not run.
})
public final class NullnessUtil {

    private NullnessUtil() {
        throw new AssertionError("shouldn't be instantiated");
    }

    /**
     * A method that suppresses warnings from the Nullness Checker.
     *
     * <p>The method takes a possibly-null reference, unsafely casts it to have the @NonNull type
     * qualifier, and returns it. The Nullness Checker considers both the return value, and also the
     * argument, to be non-null after the method call. Therefore, the {@code castNonNull} method can
     * be used either as a cast expression or as a statement. The Nullness Checker issues no
     * warnings in any of the following code:
     *
     * <pre><code>
     *   // one way to use as a cast:
     *  {@literal @}NonNull String s = castNonNull(possiblyNull1);
     *
     *   // another way to use as a cast:
     *   castNonNull(possiblyNull2).toString();
     *
     *   // one way to use as a statement:
     *   castNonNull(possiblyNull3);
     *   possiblyNull3.toString();`
     * }</code></pre>
     *
     * The {@code castNonNull} method is intended to be used in situations where the programmer
     * definitively knows that a given reference is not null, but the type system is unable to make
     * this deduction. It is not intended for defensive programming, in which a programmer cannot
     * prove that the value is not null but wishes to have an earlier indication if it is. See the
     * Checker Framework Manual for further discussion.
     *
     * <p>The method throws {@link AssertionError} if Java assertions are enabled and the argument
     * is {@code null}. If the exception is ever thrown, then that indicates that the programmer
     * misused the method by using it in a circumstance where its argument can be null.
     *
     * @param ref a reference of @Nullable type
     * @return the argument, casted to have the type qualifier @NonNull
     */
    public static @EnsuresNonNull("#1") <T extends @Nullable Object> @NonNull T castNonNull(
            @Nullable T ref) {
        assert ref != null : "Misuse of castNonNull: called with a null argument";
        return (@NonNull T) ref;
    }

    /**
     * Like castNonNull, but whereas that method only checks and casts the reference itself, this
     * traverses all levels of the argument array. The array is recursively checked to ensure that
     * all elements at every array level are non-null.
     *
     * @see #castNonNull(Object)
     */
    public static @EnsuresNonNull("#1") <T extends @Nullable Object>
            @NonNull T @NonNull [] castNonNullDeep(T @Nullable [] arr) {
        return (@NonNull T[]) castNonNullArray(arr);
    }

    /**
     * Like castNonNull, but whereas that method only checks and casts the reference itself, this
     * traverses all levels of the argument array. The array is recursively checked to ensure that
     * all elements at every array level are non-null.
     *
     * @see #castNonNull(Object)
     */
    public static @EnsuresNonNull("#1") <T extends @Nullable Object>
            @NonNull T @NonNull [][] castNonNullDeep(T @Nullable [] @Nullable [] arr) {
        return (@NonNull T[][]) castNonNullArray(arr);
    }

    /**
     * Like castNonNull, but whereas that method only checks and casts the reference itself, this
     * traverses all levels of the argument array. The array is recursively checked to ensure that
     * all elements at every array level are non-null.
     *
     * @see #castNonNull(Object)
     */
    public static @EnsuresNonNull("#1") <T extends @Nullable Object>
            @NonNull T @NonNull [][][] castNonNullDeep(T @Nullable [] @Nullable [] @Nullable [] arr) {
        return (@NonNull T[][][]) castNonNullArray(arr);
    }

    /**
     * Like castNonNull, but whereas that method only checks and casts the reference itself, this
     * traverses all levels of the argument array. The array is recursively checked to ensure that
     * all elements at every array level are non-null.
     *
     * @see #castNonNull(Object)
     */
    public static @EnsuresNonNull("#1") <T extends @Nullable Object>
            @NonNull T @NonNull [][][][] castNonNullDeep(
                    T @Nullable [] @Nullable [] @Nullable [] @Nullable [] arr) {
        return (@NonNull T[][][][]) castNonNullArray(arr);
    }

    /**
     * Like castNonNull, but whereas that method only checks and casts the reference itself, this
     * traverses all levels of the argument array. The array is recursively checked to ensure that
     * all elements at every array level are non-null.
     *
     * @see #castNonNull(Object)
     */
    public static @EnsuresNonNull("#1") <T extends @Nullable Object>
            @NonNull T @NonNull [][][][][] castNonNullDeep(
                    T @Nullable [] @Nullable [] @Nullable [] @Nullable [] @Nullable [] arr) {
        return (@NonNull T[][][][][]) castNonNullArray(arr);
    }

    private static <T extends @Nullable Object> @NonNull T @NonNull [] castNonNullArray(
            T @Nullable [] arr) {
        assert arr != null : "Misuse of castNonNullArray: called with a null array argument";
        for (int i = 0; i < arr.length; ++i) {
            assert arr[i] != null : "Misuse of castNonNull: called with a null array element";
            checkIfArray(arr[i]);
        }
        return (@NonNull T[]) arr;
    }

    private static void checkIfArray(@NonNull Object ref) {
        assert ref != null : "Misuse of checkIfArray: called with a null argument";
        Class<?> comp = ref.getClass().getComponentType();
        if (comp != null) {
            // comp is non-null for arrays, otherwise null.
            if (comp.isPrimitive()) {
                // Nothing to do for arrays of primitive type: primitives are
                // never null.
            } else {
                castNonNullArray((Object[]) ref);
            }
        }
    }
}
