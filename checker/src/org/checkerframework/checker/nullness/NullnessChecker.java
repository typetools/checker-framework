package org.checkerframework.checker.nullness;

import java.util.ArrayList;
import java.util.Collection;

import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;

/**
 * An aggregate checker for the nullness type system (with
 * freedom-before-commitment and {@link KeyFor}).
 *
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
public class NullnessChecker extends AggregateChecker {

    @Override
    protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
        Collection<Class<? extends SourceChecker>> checkers = new ArrayList<>(1);
        checkers.add(AbstractNullnessFbcChecker.class);
        return checkers;
    }

}
