package org.checkerframework.checker.experimental.regex_qual_poly;

import org.checkerframework.checker.experimental.regex_qual.Regex;
import org.checkerframework.checker.experimental.regex_qual.Regex.PartialRegex;
import org.checkerframework.checker.experimental.regex_qual.Regex.RegexVal;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.qualframework.base.CheckerAdapter;
import org.checkerframework.qualframework.poly.PolyQual.GroundQual;
import org.checkerframework.qualframework.poly.QualParams;
import org.checkerframework.qualframework.poly.QualPolyCheckerAdapter;
import org.checkerframework.qualframework.poly.QualifierParameterHierarchy;
import org.checkerframework.qualframework.poly.format.SurfaceSyntaxQualParamsFormatter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link CheckerAdapter} for the Regex-Qual-Param type system.
 */
public class RegexCheckerAdapter extends QualPolyCheckerAdapter<Regex> {

    public RegexCheckerAdapter() {
        super(new RegexQualPolyChecker());
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new RegexTypecheckVisitor(this);
    }

    @Override
    public void setupDefaults(QualifierDefaults defaults) {
        defaults.addAbsoluteDefault(
                getTypeMirrorConverter().getAnnotation(
                        new QualParams<>(new GroundQual<>(Regex.BOTTOM))),
                DefaultLocation.LOWER_BOUNDS);
        defaults.addAbsoluteDefault(
                getTypeMirrorConverter().getAnnotation(
                        new QualParams<>(new GroundQual<>(Regex.TOP))),
                DefaultLocation.LOCAL_VARIABLE);
    }

    @Override
    protected RegexSurfaceSyntaxQualParamsFormatter createSurfaceSyntaxQualParamsFormatter(
            boolean printInvisibleQualifiers) {

        return new RegexSurfaceSyntaxQualParamsFormatter(printInvisibleQualifiers);
    }

    private class RegexSurfaceSyntaxQualParamsFormatter extends SurfaceSyntaxQualParamsFormatter<Regex> {

        private final Set<String> SUPPRESS_NAMES = new HashSet<>(
                Arrays.asList("RegexTop", "RegexBottom", "PartialRegex"));

        public RegexSurfaceSyntaxQualParamsFormatter(boolean printInvisibleQualifiers) {
            super(printInvisibleQualifiers);
        }

        @Override
        protected boolean shouldPrintAnnotation(AnnotationParts anno) {
            return printInvisibleQualifiers || !(SUPPRESS_NAMES.contains(anno.getName()));
        }

        @Override
        protected AnnotationParts getTargetTypeSystemAnnotation(Regex regex) {

            if (regex instanceof RegexVal) {
                AnnotationParts anno = new AnnotationParts("Regex");
                anno.put("value", String.valueOf(((RegexVal) regex).getCount()));
                return anno;

            } else if (regex instanceof PartialRegex) {
                AnnotationParts anno = new AnnotationParts("PartialRegex");
                anno.putQuoted("value", ((PartialRegex) regex).getPartialValue());
                return anno;

            } else {
                return new AnnotationParts(regex.toString());
            }
        }

        @Override
        protected Regex getBottom() {
            return Regex.BOTTOM;
        }

        @Override
        protected Regex getTop() {
            return Regex.TOP;
        }

        @Override
        protected QualParams<Regex> getQualTop() {
            return ((QualifierParameterHierarchy<Regex>)getUnderlying().getTypeFactory().getQualifierHierarchy()).getTop();
        }

        @Override
        protected QualParams<Regex> getQualBottom() {
            return ((QualifierParameterHierarchy<Regex>)getUnderlying().getTypeFactory().getQualifierHierarchy()).getBottom();
        }
    }
}
