package org.checkerframework.checker.tainting;

import org.checkerframework.qualframework.base.Checker;

public class TaintingChecker extends Checker<Tainting> {
    @Override
    protected TaintingQualifiedTypeFactory createTypeFactory() {
        return new TaintingQualifiedTypeFactory();
    }
}
