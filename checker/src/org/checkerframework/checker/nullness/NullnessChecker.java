package org.checkerframework.checker.nullness;

import java.util.ArrayList;
import java.util.Collection;

import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.framework.source.CompoundChecker;
import org.checkerframework.framework.source.SourceChecker;

/**
 * An aggregated checker for the nullness type-system (with
 * freedom-before-commitment) and {@link KeyFor}).
 */
public class NullnessChecker extends CompoundChecker {

    @Override
    protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
        Collection<Class<? extends SourceChecker>> checkers = new ArrayList<>(2);
        checkers.add(KeyForSubchecker.class);
        checkers.add(AbstractNullnessFbcChecker.class);
        return checkers;
    }

}
