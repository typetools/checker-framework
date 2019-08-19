package org.checkerframework.common.purity;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/**
 * Perform purity checking only.
 *
 * @checker_framework.manual #type-refinement-purity Side effects, determinism, purity, and
 *     flow-sensitive analysis
 */
public class PurityChecker extends BaseTypeChecker {

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new PurityVisitor(this);
    }
}
