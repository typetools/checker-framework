package org.checkerframework.framework.testchecker.sideeffectsonly;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;

/**
 * Toy checker used to test whether dataflow analysis correctly type-refines methods annotated with
 * {@link org.checkerframework.dataflow.qual.SideEffectsOnly}.
 */
public class SideEffectsOnlyToyChecker extends BaseTypeChecker {
    /**
     * Sets {@code sideEffectsUnrefineAliases} to true as {@code @SideEffectsOnly} annotation has
     * effect only on methods that unrefine types.
     *
     * @return GenericAnnotatedTypeFactory
     */
    @Override
    public GenericAnnotatedTypeFactory<?, ?, ?, ?> getTypeFactory() {
        GenericAnnotatedTypeFactory<?, ?, ?, ?> result = super.getTypeFactory();
        result.sideEffectsUnrefineAliases = true;
        return result;
    }
}
