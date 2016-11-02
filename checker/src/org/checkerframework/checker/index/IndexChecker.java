package org.checkerframework.checker.index;

import java.util.Arrays;
import java.util.Collection;
import org.checkerframework.checker.lowerbound.LowerBoundChecker;
import org.checkerframework.checker.upperbound.UpperBoundChecker;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;

/**
 * A type-checker for preventing arrays from being accessed with values that are too low. Normally
 * bundled as part of the Index Checker.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
public class IndexChecker extends AggregateChecker {
    protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
        return Arrays.asList(
                ((Class<? extends SourceChecker>) LowerBoundChecker.class),
                UpperBoundChecker.class);
    }
}

//public class IndexChecker {}
