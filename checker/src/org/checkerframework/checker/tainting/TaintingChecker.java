package org.checkerframework.checker.tainting;

import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.qualframework.base.CheckerAdapter;
import org.checkerframework.qualframework.base.TypecheckVisitorAdapter;
import org.checkerframework.qualframework.poly.PolyQual.GroundQual;
import org.checkerframework.qualframework.poly.QualParams;

public class TaintingChecker extends CheckerAdapter<QualParams<Tainting>> {
    public TaintingChecker() {
        super(new TaintingQualChecker());
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new TypecheckVisitorAdapter<>(this);
    }

    @Override
    public void setupDefaults(QualifierDefaults defaults) {
        defaults.addCheckedCodeDefault(
                getTypeMirrorConverter().getAnnotation(
                        new QualParams<>(new GroundQual<>(Tainting.UNTAINTED))),
                TypeUseLocation.IMPLICIT_LOWER_BOUND);

        defaults.addCheckedCodeDefault(
                getTypeMirrorConverter().getAnnotation(
                        new QualParams<>(new GroundQual<>(Tainting.TAINTED))),
                TypeUseLocation.LOCAL_VARIABLE);
    }

}
