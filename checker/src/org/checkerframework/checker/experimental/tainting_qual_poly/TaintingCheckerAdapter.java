package org.checkerframework.checker.experimental.tainting_qual_poly;

import org.checkerframework.checker.experimental.regex_qual.Regex;
import org.checkerframework.checker.experimental.regex_qual.Regex.PartialRegex;
import org.checkerframework.checker.experimental.regex_qual.Regex.RegexVal;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.qualframework.base.CheckerAdapter;
import org.checkerframework.qualframework.base.TypecheckVisitorAdapter;
import org.checkerframework.qualframework.poly.PolyQual.GroundQual;
import org.checkerframework.qualframework.poly.QualParams;
import org.checkerframework.qualframework.poly.QualPolyCheckerAdapter;
import org.checkerframework.qualframework.poly.QualifierParameterHierarchy;
import org.checkerframework.qualframework.poly.format.SurfaceSyntaxQualParamsFormatter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
            super(printInvisibleQualifiers);
        }

        @Override
        protected boolean shouldPrintAnnotation(AnnotationParts anno) {
            return printInvisibleQualifiers;
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

        @Override
        protected Tainting getBottom() {
            return Tainting.UNTAINTED;
        }

        @Override
        protected Tainting getTop() {
            return Tainting.TAINTED;
        }

        @Override
        protected QualParams<Tainting> getQualTop() {
            return ((QualifierParameterHierarchy<Tainting>)getUnderlying().getTypeFactory().getQualifierHierarchy()).getTop();
        }

        @Override
        protected QualParams<Tainting> getQualBottom() {
            return ((QualifierParameterHierarchy<Tainting>)getUnderlying().getTypeFactory().getQualifierHierarchy()).getBottom();
        }
    }
}
