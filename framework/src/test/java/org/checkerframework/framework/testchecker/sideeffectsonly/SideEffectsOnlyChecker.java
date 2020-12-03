package org.checkerframework.framework.testchecker.sideeffectsonly;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;

public class SideEffectsOnlyChecker extends BaseTypeChecker {
    @Override
    public GenericAnnotatedTypeFactory<?, ?, ?, ?> getTypeFactory() {
        GenericAnnotatedTypeFactory<?, ?, ?, ?> result = super.getTypeFactory();
        result.sideEffectsUnrefineAliases = true;
        return result;
    }
}
