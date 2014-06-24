package org.checkerframework.checker.tainting;

import org.checkerframework.qualframework.base.Checker;

import org.checkerframework.qualframework.poly.QualParams;

public class TaintingChecker extends Checker<QualParams<Tainting>> {
    @Override
    protected TaintingQualifiedTypeFactory createTypeFactory() {
        return new TaintingQualifiedTypeFactory();
    }
}
