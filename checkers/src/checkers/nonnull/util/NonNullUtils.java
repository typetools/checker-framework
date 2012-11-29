package checkers.nonnull.util;

import checkers.initialization.quals.*;
import checkers.nullness.quals.AssertParametersNonNull;

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
public final class NonNullUtils {

    private NonNullUtils()
    { throw new AssertionError("shouldn't be instantiated"); }

    /**
     * A method that suppresses warnings from the Nullness Checker.
     * <p>
     *
     * The method takes a possibly-null reference, unsafely casts it to
     * have the @checkers.nonnull.quals.NonNull type qualifier, and returns it.  The Nullness
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
     * @return the argument, casted to have the type qualifier @checkers.nonnull.quals.NonNull
     */
    @SuppressWarnings("nullness")
    @AssertParametersNonNull
    public static <T extends /*@checkers.nonnull.quals.Nullable*/ Object> /*@checkers.nonnull.quals.NonNull*/ T castNonNull(T ref) {
        assert ref != null : "misuse of castNonNull, which should never be called on a null argument";
        return (/*@checkers.nonnull.quals.NonNull*/ T)ref;
    }

    @SuppressWarnings("nullness")
    @AssertParametersNonNull
    public static <T extends /*@checkers.nonnull.quals.Nullable*/ Object> /*@checkers.nonnull.quals.NonNull*/ T castNonNullFbc(T ref) {
        assert ref != null : "misuse of castNonNull, which should never be called on a null argument";
        return (/*@checkers.nonnull.quals.NonNull*/ T)ref;
    }

    @SuppressWarnings("nullness")
    @AssertParametersNonNull
    public static <T extends /*@checkers.nonnull.quals.Nullable*/ Object> /*@checkers.nonnull.quals.NonNull*/ T castKeyFor(T ref) {
        assert ref != null : "misuse of castNonNull, which should never be called on a null argument";
        return (/*@checkers.nonnull.quals.NonNull*/ T)ref;
    }

    @SuppressWarnings("nullness")
    @AssertParametersNonNull
    public static <T extends /*@checkers.nonnull.quals.Nullable*/ Object> /*@checkers.nonnull.quals.NonNull*/ T castTypeState(T ref) {
        assert ref != null : "misuse of castNonNull, which should never be called on a null argument";
        return (/*@checkers.nonnull.quals.NonNull*/ T)ref;
    }


    @SuppressWarnings("nullness")
    public static <T extends Object> /*@checkers.nonnull.quals.Nullable*/ T castKnownNull(T ref) {
        assert ref != null : "misuse of castNonNull, which should never be called on a null argument";
        return ref;
    }

    @SuppressWarnings("nullness")
    public static <T extends Object> /*@checkers.nonnull.quals.Nullable*/ T castKnownNull2(T ref) {
        assert ref != null : "misuse of castNonNull, which should never be called on a null argument";
        return ref;
    }

    @SuppressWarnings("rawness")
    public static <T extends /*@checkers.initialization.quals.Raw*/ /*@Unclassified*/ Object> /*@checkers.initialization.quals.NonRaw*/ T castNonRaw(T ref) {
        return (/*@checkers.initialization.quals.NonRaw*/ T)ref;
    }

    @SuppressWarnings("rawness")
    public static <T extends /*@checkers.initialization.quals.Raw*/ /*@Unclassified*/ Object> /*@checkers.initialization.quals.NonRaw*/ /*@Committed*/ T castFrame(T ref) {
        return (/*@checkers.initialization.quals.NonRaw*/ /*@Committed*/ T)ref;
    }

    // for if you know it's in the list (getSelectedIndex) or if hasNext() & .next() is called, menuBar.getMenu
    @SuppressWarnings("nullness")
    @AssertParametersNonNull
    public static <T extends /*@checkers.nonnull.quals.Nullable*/ Object> /*@checkers.nonnull.quals.NonNull*/ T castListIndex(T ref) {
        assert ref != null : "misuse of castNonNull, which should never be called on a null argument";
        return (/*@checkers.nonnull.quals.NonNull*/ T)ref;
    }

    @SuppressWarnings("nullness")
    @AssertParametersNonNull
    public static <T extends /*@checkers.nonnull.quals.Nullable*/ Object> /*@checkers.nonnull.quals.NonNull*/ T castWeird(T ref) {
        assert ref != null : "misuse of castNonNull, which should never be called on a null argument";
        return (/*@checkers.nonnull.quals.NonNull*/ T)ref;
    }

    // flow cannot figure out something
    @SuppressWarnings("nullness")
    @AssertParametersNonNull
    public static <T extends /*@checkers.nonnull.quals.Nullable*/ Object> /*@checkers.nonnull.quals.NonNull*/ T castFlow(T ref) {
        assert ref != null : "misuse of castNonNull, which should never be called on a null argument";
        return (/*@checkers.nonnull.quals.NonNull*/ T)ref;
    }

    // access some attributes of an element of a config file (domain specific knowledge)
    // depending on structure of XML file being parsed, certain fields are known to be set
    @SuppressWarnings("nullness")
    @AssertParametersNonNull
    public static <T extends /*@checkers.nonnull.quals.Nullable*/ Object> /*@checkers.nonnull.quals.NonNull*/ T castDomain(T ref) {
        assert ref != null : "misuse of castNonNull, which should never be called on a null argument";
        return (/*@checkers.nonnull.quals.NonNull*/ T)ref;
    }


    // this seems like a bug, but our confidence is not very high about that
    @SuppressWarnings("nullness")
    @AssertParametersNonNull
    public static <T extends /*@checkers.nonnull.quals.Nullable*/ Object> /*@checkers.nonnull.quals.NonNull*/ T castUnsafe(T ref) {
        assert ref != null : "misuse of castNonNull, which should never be called on a null argument";
        return (/*@checkers.nonnull.quals.NonNull*/ T)ref;
    }

    // definitely looks like a bug
    @SuppressWarnings("nullness")
    @AssertParametersNonNull
    public static <T extends /*@checkers.nonnull.quals.Nullable*/ Object> /*@checkers.nonnull.quals.NonNull*/ T castBug(T ref) {
        assert ref != null : "misuse of castNonNull, which should never be called on a null argument";
        return (/*@checkers.nonnull.quals.NonNull*/ T)ref;
    }

    /**
     * For use only in GUI listener code.
     * @param ref
     * @return ref with NonRaw and Committed annotations
     */
    @SuppressWarnings({"rawness","commitment"})
    public static <T extends /*@checkers.initialization.quals.Raw*/ /*@Unclassified*/ Object> /*@checkers.initialization.quals.NonRaw*/ /*@Committed*/ T castInitGui(T ref) {
        return (/*@checkers.initialization.quals.NonRaw*/ /*@Committed*/ T)ref;
    }

    /**
     * For final-factory cases, not counted
     */
    @SuppressWarnings({"rawness","commitment"})
    public static <T extends /*@checkers.initialization.quals.Raw*/ /*@Unclassified*/ Object> /*@checkers.initialization.quals.NonRaw*/ /*@Committed*/ T castFactory(T ref) {
        return (/*@checkers.initialization.quals.NonRaw*/ /*@Committed*/ T)ref;
    }

    // System.getProperty (should really be a different type system for that)
    @SuppressWarnings("nullness")
    @AssertParametersNonNull
    public static <T extends /*@checkers.nonnull.quals.Nullable*/ Object> /*@checkers.nonnull.quals.NonNull*/ T castSystemProperty(T ref) {
        assert ref != null : "misuse of castNonNull, which should never be called on a null argument";
        return (/*@checkers.nonnull.quals.NonNull*/ T)ref;
    }

    // application invariant. uses so far:
    // - map that always contains key username
    // - SSH_FILEXFER_ATTR_UIDGID set -> nonnull field gid
    @SuppressWarnings("nullness")
    @AssertParametersNonNull
    public static <T extends /*@checkers.nonnull.quals.Nullable*/ Object> /*@checkers.nonnull.quals.NonNull*/ T castAI(T ref) {
        assert ref != null : "misuse of castNonNull, which should never be called on a null argument";
        return (/*@checkers.nonnull.quals.NonNull*/ T)ref;
    }

    // known state of filesystem guarantees if you pass a valid input it returns null (getIcon)
    @SuppressWarnings("nullness")
    @AssertParametersNonNull
    public static <T extends /*@checkers.nonnull.quals.Nullable*/ Object> /*@checkers.nonnull.quals.NonNull*/ T castKnownFilesystem(T ref) {
        assert ref != null : "misuse of castNonNull, which should never be called on a null argument";
        return (/*@checkers.nonnull.quals.NonNull*/ T)ref;
    }

    // for cases where PolyNull would really be best
    @SuppressWarnings("nullness")
    @AssertParametersNonNull
    public static <T extends /*@checkers.nonnull.quals.Nullable*/ Object> /*@checkers.nonnull.quals.NonNull*/ T castPoly(T ref) {
        assert ref != null : "misuse of castNonNull, which should never be called on a null argument";
        return (/*@checkers.nonnull.quals.NonNull*/ T)ref;
    }

    // array that really should be lazynonnull
    @SuppressWarnings("nullness")
    @AssertParametersNonNull
    public static <T extends /*@checkers.nonnull.quals.Nullable*/ Object> /*@checkers.nonnull.quals.NonNull*/ T castArray(T ref) {
        assert ref != null : "misuse of castNonNull, which should never be called on a null argument";
        return (/*@checkers.nonnull.quals.NonNull*/ T)ref;
    }


}
