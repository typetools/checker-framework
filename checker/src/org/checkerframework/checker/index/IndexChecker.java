package org.checkerframework.checker.index;

import org.checkerframework.checker.index.lowerbound.LowerBoundChecker;

/**
 * A type checker for preventing out-of-bounds accesses on arrays. Contains two subcheckers that do
 * all of the actual work: the Lower Bound Checker and the Upper Bound Checker.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
public class IndexChecker extends LowerBoundChecker {
    // Ideally, the Index Checker would be an AggregateChecker that ran the Lower and Upper Bound
    // Checkers.  However, both checkers use annotations from the ValueChecker and the
    // MinLenChecker. So in order for these subcheckers to only be run once, the IndexChecker
    // is a subclass of the LowerBoundChecker.  If isIndexChecker() returns true, then the
    // LowerBoundChecker runs the UpperBoundChecker as a subchecker.

    @Override
    protected boolean isIndexChecker() {
        return true;
    }
}
