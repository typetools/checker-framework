package org.checkerframework.checker.index;

import org.checkerframework.checker.lowerbound.LowerBoundChecker;

/**
 * A type checker for preventing out-of-bounds accesses on arrays and lists. Contains two
 * subcheckers that do all of the actual work: the Lower Bound Checker and the Upper Bound Checker.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
public class IndexChecker extends LowerBoundChecker {
    // This class is literally just the Lower Bound Checker right now. The Lower Bound Checker runs
    // the Upper Bound Checker as a subchecker at the moment, because this class can't run both as
    // it's subcheckers while retaining the benefits of being a compound checker - basically, what
    // we'd like is an aggregate checker that will run its subcheckers each only once in an order
    // that makes sense.
}
