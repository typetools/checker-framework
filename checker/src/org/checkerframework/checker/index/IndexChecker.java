package org.checkerframework.checker.index;

import org.checkerframework.checker.index.upperbound.UpperBoundChecker;

/**
 * A type checker for preventing out-of-bounds accesses on arrays. Contains two subcheckers that do
 * all of the actual work: the Lower Bound Checker and the Upper Bound Checker.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
public class IndexChecker extends UpperBoundChecker {
    // Ideally, the Index Checker would be an AggregateChecker that ran the Lower and Upper Bound
    // Checkers.  However, both checkers use annotations from the ValueChecker and the
    // MinLenChecker. So in order for these subcheckers to only be run once, the IndexChecker
    // is a subclass of the UpperBoundChecker, which runs the LowerBoundChecker as a subchecker
    // anyway.

    // TODO: we should discuss what's actually going on here, because at this point the Index Checker
    // now exists only for the name, I think.
}
