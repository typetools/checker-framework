package checkers.nullness;

import checkers.nullness.quals.*;

/**
 * Utilities class for the Nullness Checker.
 *
 * <b>Runtime Dependency</b>
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
    { throw new AssertionError("shouldn't be intiantiated"); }

    /**
     * A nullness suppression method.  It retrieves a nonnull reference
     * from a potentially nullable one, while suppressing the Nullness
     * checker for the unsafe cast.
     *
     * The method throws an {@link AssertionError} if Java assertions are
     * enabled and the argument is a {@code null}.
     *
     * @param a reference whose nullness is unknown
     * @return the passed reference as a nonnull one
     */
    @SuppressWarnings("nullness")
    public <T extends @Nullable Object> @NonNull T swNonNull(T ref) {
        assert ref != null : "ref cannot be null";
        return (@NonNull T)ref;
    }
}
