package org.checkerframework.checker.oigj;

import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A type-checker plug-in for the OIGJ immutability type system that finds (and
 * verifies the absence of) undesired side-effect errors.
 *
 * The OIGJ language is a Java language extension that expresses immutability
 * and ownership constraints.
 *
 * <!-- TODO: reinstate once manual chapter exists: @checker_framework.manual #oigj-checker OIGJ Checker -->
 *
 */
public class OIGJChecker extends AggregateChecker {

    @Override
    protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
        Collection<Class<? extends SourceChecker>> checkers = new ArrayList<>(2);
        checkers.add(ImmutabilitySubchecker.class);
        checkers.add(OwnershipSubchecker.class);
        return checkers;
    }
}
