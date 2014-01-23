package org.checkerframework.checkers.tainting;

import org.checkerframework.framework.base.Checker;

public class TaintingChecker extends Checker<Tainting> {
    @Override
    protected TaintingQualifiedTypeFactory createTypeFactory() {
        return new TaintingQualifiedTypeFactory();
    }
}
