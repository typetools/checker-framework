package checkers.nullness;

import java.util.ArrayList;
import java.util.Collection;

import checkers.nullness.quals.*;
import checkers.source.*;
import checkers.util.AggregateChecker;

/**
 * A typechecker plug-in for the Rawness type system qualifier that finds (and
 * verifies the absence of) null-pointer errors.
 *
 * @see NonNull
 * @see Nullable
 * @see Raw
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
public class NullnessChecker extends AggregateChecker {

    @Override
    protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
        Collection<Class<? extends SourceChecker>> checkers = new ArrayList<Class<? extends SourceChecker>>(2);
        checkers.add(NullnessSubchecker.class);
        checkers.add(RawnessSubchecker.class);
        checkers.add(KeyForSubchecker.class);
        return checkers;
    }
}
