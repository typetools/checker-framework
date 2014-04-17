package org.checkerframework.checker.tainting;

import org.checkerframework.qualframework.base.Checker;

import org.checkerframework.checker.qualparam.QualParams;

public class TaintingChecker extends Checker<QualParams<Tainting>> {
    @Override
    protected TaintingQualifiedTypeFactory createTypeFactory() {
        return new TaintingQualifiedTypeFactory();
    }
}
