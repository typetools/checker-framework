package org.checkerframework.checker.experimental.tainting_qual_poly;

import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.qualframework.base.CheckerAdapter;
import org.checkerframework.qualframework.base.TypecheckVisitorAdapter;
import org.checkerframework.qualframework.poly.PolyQual.GroundQual;
import org.checkerframework.qualframework.poly.QualParams;

public class TaintingCheckerAdapter extends CheckerAdapter<QualParams<Tainting>> {
    public TaintingCheckerAdapter() {
        super(new TaintingChecker());
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new TypecheckVisitorAdapter<>(this);
    }

    @Override
    public void setupDefaults(QualifierDefaults defaults) {
        defaults.addAbsoluteDefault(
                getTypeMirrorConverter().getAnnotation(
                        new QualParams<>(new GroundQual<>(Tainting.UNTAINTED))),
                DefaultLocation.IMPLICIT_LOWER_BOUNDS);

        defaults.addAbsoluteDefault(
                getTypeMirrorConverter().getAnnotation(
                        new QualParams<>(new GroundQual<>(Tainting.TAINTED))),
                DefaultLocation.LOCAL_VARIABLE);
    }

}
