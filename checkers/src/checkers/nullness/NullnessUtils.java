package checkers.nullness;

import checkers.nullness.quals.*;

/**
 * Utilities class for the Nullness Checker.
 * <p>
 *
 * To avoid the need to write the NullnessUtils class name, do:
 * <pre>import static checkers.nullness.NullnessUtils.castNonNull;</pre>
 * <p>
 *
 * <b>Runtime Dependency</b>
 * <p>
 *
 * Please note that using this class introduces a Runtime dependency.
 * This means that if you need to distribute (or link to) the Checker
 * Framework, along with your binaries.
 *
 * To eliminate this dependency, you can simply copy this class into your
 * own project.
 */
public final class NullnessUtils {

    private NullnessUtils()
    { throw new AssertionError("shouldn't be instantiated"); }

    /**
     * A method that suppresses warnings from the Nullness Checker.
     * <p>
     *
     * The method takes a possibly-null reference, unsafely casts it to
     * have the @NonNull type qualifier, and returns it.  The Nullness
     * Checker considers both the return value, and also the argument, to
     * be non-null after the method call.  Therefore, the
     * <tt>castNonNull</tt> method can be used either as a cast expression or
     * as a statement.  The Nullness Checker issues no warnings in any of
     * the following code:
     *
     * <pre>
     *   // one way to use as a cast:
     *   &#64;NonNull String s = castNonNull(possiblyNull1);
     *
     *   // another way to use as a cast:
     *   castNonNull(possiblyNull2).toString();
     *
     *   // one way to use as a statement:
     *   castNonNull(possiblyNull3);
     *   possiblyNull3.toString();`
     * </pre>
     *
     * The <tt>castNonNull</tt> method is intended to be used in situations
     * where the programmer definitively knows that a given reference is
     * not null, but the type system is unable to make this deduction.  It
     * is not intended for defensive programming, in which a programmer
     * cannot prove that the value is not null but wishes to have an
     * earlier indication if it is.  See the Checker Framework manual for
     * further discussion.
     * <p>
     *
     * The method throws {@link AssertionError} if Java assertions are
     * enabled and the argument is {@code null}.  If the exception is ever
     * thrown, then that indicates that the programmer misused the method
     * by using it in a circumstance where its argument can be null.
     * <p>
     *
     * @param ref a possibly-null reference
     * @return the argument, casted to have the type qualifier @NonNull
     */
    @SuppressWarnings("nullness")
    @AssertParametersNonNull
    public static <T extends /*@Nullable*/ Object> /*@NonNull*/ T castNonNull(T ref) {
        assert ref != null : "misuse of castNonNull, which should never be called on a null argument";
        return (/*@NonNull*/ T)ref;
    }
}
