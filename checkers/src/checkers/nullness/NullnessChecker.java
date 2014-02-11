package checkers.nullness;

import checkers.nullness.quals.KeyFor;
import checkers.source.AggregateChecker;
import checkers.source.SourceChecker;

import java.util.ArrayList;
import java.util.Collection;

/**
 * An aggregated checker for the nullness type-system (with
 * freedom-before-commitment) and {@link KeyFor}).
 */
public class NullnessChecker extends AggregateChecker {

    @Override
    protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
        Collection<Class<? extends SourceChecker>> checkers = new ArrayList<>();
        checkers.add(AbstractNullnessFbcChecker.class);
        checkers.add(KeyForSubchecker.class);
        return checkers;
    }

}
