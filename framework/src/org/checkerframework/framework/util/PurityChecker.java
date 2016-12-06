package org.checkerframework.framework.util;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * Perform purity checking only.
 *
 * @checker_framework.manual #type-refinement-purity Side effects, determinism, purity, and
 *     flow-sensitive analysis
 */
public class PurityChecker extends BaseTypeChecker {
    // There is no implementation here.
    // It uses functionality from BaseTypeChecker, which itself calls
    // dataflow's purity implementation.
}
