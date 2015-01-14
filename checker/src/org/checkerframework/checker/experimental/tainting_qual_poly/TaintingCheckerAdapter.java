package org.checkerframework.checker.experimental.tainting_qual_poly;

import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.qualframework.base.TypecheckVisitorAdapter;
import org.checkerframework.qualframework.poly.PolyQual.GroundQual;
import org.checkerframework.qualframework.poly.QualParams;
import org.checkerframework.qualframework.poly.QualPolyCheckerAdapter;
import org.checkerframework.qualframework.poly.format.SurfaceSyntaxQualParamsFormatter;

public class TaintingCheckerAdapter extends QualPolyCheckerAdapter<Tainting> {
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

    @Override
    protected TaintingSurfaceSyntaxQualParamsFormatter createSurfaceSyntaxQualParamsFormatter(
            boolean printInvisibleQualifiers) {

        return new TaintingSurfaceSyntaxQualParamsFormatter(printInvisibleQualifiers);
    }

    private class TaintingSurfaceSyntaxQualParamsFormatter extends SurfaceSyntaxQualParamsFormatter<Tainting> {

        public TaintingSurfaceSyntaxQualParamsFormatter(boolean printInvisibleQualifiers) {
            super(printInvisibleQualifiers, Tainting.TAINTED, Tainting.UNTAINTED,
                    getUnderlying().getTypeFactory().getQualifierHierarchy().getTop(),
                    getUnderlying().getTypeFactory().getQualifierHierarchy().getBottom());
        }

        @Override
        protected boolean shouldPrintAnnotation(AnnotationParts anno) {
            return true;
        }

        @Override
        protected AnnotationParts getTargetTypeSystemAnnotation(Tainting regex) {

            switch(regex) {
                case TAINTED:
                    return new AnnotationParts("Tainted");
                case UNTAINTED:
                    return new AnnotationParts("Untainted");
                default:
                    return null; // Dead code
            }
        }
    }
}
