package org.checkerframework.checker.nullness;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.checkerframework.checker.initialization.InitializationChecker;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

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
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        LinkedHashSet<Class<? extends BaseTypeChecker>> checkers
            = new LinkedHashSet<Class<? extends BaseTypeChecker>>(1);
        checkers.add(KeyForSubchecker.class);
    	return checkers;
    }

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
