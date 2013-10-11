package checkers.nullness;

import checkers.basetype.BaseTypeVisitor;
import checkers.initialization.InitializationChecker;
import checkers.nullness.quals.MonotonicNonNull;

import java.util.Collection;
import java.util.HashSet;

/**
 * An implementation of the nullness type-system based on an initialization
 * type-system for safe initialization.
 */
public abstract class AbstractNullnessChecker extends InitializationChecker {

    /**
     * Should we be strict about initialization of {@link MonotonicNonNull} variables.
     */
    public static final String LINT_NOINITFORMONOTONICNONNULL = "noInitForMonotonicNonNull";

    /**
     * Default for {@link #LINT_NOINITFORMONOTONICNONNULL}.
     */
    public static final boolean LINT_DEFAULT_NOINITFORMONOTONICNONNULL = false;

    /**
     * Warn about redundant comparisons of expressions with {@code null}, if the
     * expressions is known to be non-null.
     */
    public static final String LINT_REDUNDANTNULLCOMPARISON = "redundantNullComparison";

    /**
     * Default for {@link #LINT_REDUNDANTNULLCOMPARISON}.
     */
    public static final boolean LINT_DEFAULT_REDUNDANTNULLCOMPARISON = false;

    public AbstractNullnessChecker(boolean useFbc) {
        super(useFbc);
    }

    /*
    @Override
    public void initChecker() {
        super.initChecker();
    }
    */

    @Override
    public Collection<String> getSuppressWarningsKeys() {
        Collection<String> result = new HashSet<>(super.getSuppressWarningsKeys());
        result.add("nullness");
        return result;
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new NullnessVisitor(this, useFbc);
    }
}
