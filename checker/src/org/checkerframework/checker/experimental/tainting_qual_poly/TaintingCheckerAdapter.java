package org.checkerframework.checker.experimental.tainting_qual_poly;

import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.qualframework.base.CheckerAdapter;

import org.checkerframework.qualframework.base.TypecheckVisitorAdapter;
import org.checkerframework.qualframework.poly.QualParams;

public class TaintingCheckerAdapter extends CheckerAdapter<QualParams<Tainting>> {
    public TaintingCheckerAdapter() {
        super(new TaintingChecker());
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new TypecheckVisitorAdapter<>(this);
    }
}
