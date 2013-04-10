package checkers.nullness;

import java.util.ArrayList;
import java.util.Collection;

import checkers.nullness.quals.KeyFor;
import checkers.source.AggregateChecker;
import checkers.source.SourceChecker;

/**
 * An aggregated checker for the nullness type-system (with rawness) and
 * {@link KeyFor}.
 */
public class NullnessRawnessChecker extends AggregateChecker {

    @Override
    protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
        Collection<Class<? extends SourceChecker>> checkers = new ArrayList<>();
        checkers.add(AbstractNullnessRawnessChecker.class);
        checkers.add(KeyForSubchecker.class);
        return checkers;
    }

}
