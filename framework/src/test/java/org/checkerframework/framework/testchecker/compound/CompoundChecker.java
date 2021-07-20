package org.checkerframework.framework.testchecker.compound;

import org.checkerframework.common.aliasing.AliasingChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

import java.util.LinkedHashSet;

/**
 * Used to test the compound checker design pattern. AliasingChecker and AnotherCompoundChecker are
 * subcheckers of this checker AnotherCompoundChecker relies on the Aliasing Checker, too. This is
 * so that the order of subcheckers is tested.
 */
public class CompoundChecker extends BaseTypeChecker {
    @Override
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        LinkedHashSet<Class<? extends BaseTypeChecker>> subcheckers = new LinkedHashSet<>();
        subcheckers.addAll(super.getImmediateSubcheckerClasses());
        subcheckers.add(AliasingChecker.class);
        subcheckers.add(AnotherCompoundChecker.class);
        return subcheckers;
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new BaseTypeVisitor<CompoundCheckerAnnotatedTypeFactory>(this) {
            @Override
            protected CompoundCheckerAnnotatedTypeFactory createTypeFactory() {
                return new CompoundCheckerAnnotatedTypeFactory(checker);
            }
        };
    }
}
