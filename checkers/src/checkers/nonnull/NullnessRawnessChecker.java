package checkers.nonnull;

import java.util.ArrayList;
import java.util.Collection;

import checkers.nullness.KeyForSubchecker;
import checkers.source.AggregateChecker;
import checkers.source.SourceChecker;

public class NullnessRawnessChecker extends AggregateChecker {

    @Override
    protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
        Collection<Class<? extends SourceChecker>> checkers = new ArrayList<>();
        checkers.add(AbstractNullnessRawnessChecker.class);
        checkers.add(KeyForSubchecker.class);
        return checkers;
    }

}
