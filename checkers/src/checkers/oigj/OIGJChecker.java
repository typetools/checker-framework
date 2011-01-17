package checkers.oigj;

import java.util.ArrayList;
import java.util.Collection;

import checkers.source.SourceChecker;
import checkers.util.AggregateChecker;

/**
 * A type-checker plug-in for the OIGJ immutability type system that finds (and
 * verifies the absence of) undesired side-effect errors.
 *
 * The OIGJ language is a Java language extension that expresses immutability
 * and ownership constraints.
 *
 * <!-- TODO: reinstate once manual chapter exists: @checker.framework.manual #oigj-checker OIGJ Checker -->
 *
 */
public class OIGJChecker extends AggregateChecker {

    @Override
    protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
        Collection<Class<? extends SourceChecker>> checkers
            = new ArrayList<Class<? extends SourceChecker>>();
        checkers.add(ImmutabilitySubchecker.class);
        checkers.add(OwnershipSubchecker.class);
        return checkers;
    }
}
