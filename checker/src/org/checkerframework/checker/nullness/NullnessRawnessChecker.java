package org.checkerframework.checker.nullness;

import java.util.ArrayList;
import java.util.Collection;

import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;

/**
 * An aggregated checker for the nullness type-system (with rawness) and
 * {@link KeyFor}.
 */
public class NullnessRawnessChecker extends AggregateChecker {

    @Override
    protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
        Collection<Class<? extends SourceChecker>> checkers = new ArrayList<>(1);
        checkers.add(AbstractNullnessRawnessChecker.class);
        return checkers;
    }

}
